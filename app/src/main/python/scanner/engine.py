"""Scanner engine for T0H1 Scanner.

This module deliberately keeps the core networking in Python while the Android
side owns the UI, state management, persistence, and coroutine orchestration.
"""

from __future__ import annotations

import concurrent.futures
import csv
import ipaddress
import json
import os
import socket
import statistics
import threading
import time
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Iterable, Any

DEFAULT_TIMEOUT = 3.0
DEFAULT_CONCURRENCY = 20


def _json_loads_maybe(value: Any):
    if isinstance(value, (list, dict)):
        return value
    if value is None:
        return []
    if isinstance(value, bytes):
        value = value.decode("utf-8", errors="ignore")
    if isinstance(value, str):
        text = value.strip()
        if not text:
            return []
        try:
            return json.loads(text)
        except Exception:
            return text
    return value


def _target_line_to_record(line: str) -> dict | None:
    line = (line or "").strip()
    if not line or line.startswith("#") or line.startswith(";"):
        return None

    parts = [p.strip() for p in line.split("|")]
    endpoint = parts[0]
    region = parts[1] if len(parts) > 1 else ""
    notes = parts[2] if len(parts) > 2 else ""

    host = endpoint
    port = None

    if endpoint.startswith("[") and "]" in endpoint:
        host = endpoint[1:endpoint.index("]")]
        tail = endpoint[endpoint.index("]") + 1 :]
        if tail.startswith(":"):
            try:
                port = int(tail[1:])
            except Exception:
                port = None
    elif endpoint.count(":") == 1 and not endpoint.startswith("http"):
        maybe_host, maybe_port = endpoint.rsplit(":", 1)
        if maybe_port.isdigit():
            host, port = maybe_host, int(maybe_port)
    elif "," in endpoint:
        maybe = [x.strip() for x in endpoint.split(",") if x.strip()]
        if len(maybe) >= 2 and maybe[-1].isdigit():
            host, port = ",".join(maybe[:-1]), int(maybe[-1])

    return {
        "target": host,
        "explicit_port": port,
        "region": region,
        "notes": notes,
    }


def _normalize_targets(targets: Any) -> list[dict]:
    data = _json_loads_maybe(targets)

    records: list[dict] = []
    if isinstance(data, list):
        for item in data:
            if isinstance(item, str):
                rec = _target_line_to_record(item)
                if rec:
                    records.append(rec)
            elif isinstance(item, dict):
                records.append(
                    {
                        "target": str(item.get("target") or item.get("host") or "").strip(),
                        "explicit_port": item.get("explicit_port"),
                        "region": str(item.get("region") or "").strip(),
                        "notes": str(item.get("notes") or "").strip(),
                    }
                )
    elif isinstance(data, str):
        for line in data.splitlines():
            rec = _target_line_to_record(line)
            if rec:
                records.append(rec)

    # Deduplicate by target/port/region/notes.
    seen = set()
    unique = []
    for rec in records:
        key = (
            rec["target"].lower(),
            rec.get("explicit_port"),
            rec["region"].lower(),
            rec["notes"].lower(),
        )
        if key not in seen:
            seen.add(key)
            unique.append(rec)
    return unique


def _normalize_ports(ports: Any) -> list[int]:
    data = _json_loads_maybe(ports)
    result: list[int] = []

    if isinstance(data, list):
        tokens = []
        for item in data:
            if isinstance(item, int):
                tokens.append(item)
            else:
                tokens.extend(str(item).replace(";", ",").split(","))
    else:
        tokens = str(data).replace("\n", ",").replace(";", ",").split(",")

    for token in tokens:
        text = str(token).strip()
        if not text:
            continue
        try:
            value = int(text)
        except Exception:
            continue
        if 1 <= value <= 65535:
            result.append(value)

    # dedupe while keeping order
    out = []
    seen = set()
    for p in result:
        if p not in seen:
            seen.add(p)
            out.append(p)
    return out


def _is_ip_address(host: str) -> bool:
    try:
        ipaddress.ip_address(host)
        return True
    except Exception:
        return False


def _resolve_host(host: str) -> list[tuple]:
    if not host:
        return []
    try:
        infos = socket.getaddrinfo(host, None, type=socket.SOCK_STREAM)
        return infos
    except Exception:
        return []


def _connect_latency(host: str, port: int, timeout: float) -> dict:
    """
    Resolve and TCP-connect to the first usable address. Returns a JSON-compatible dict.
    """
    host = host.strip()
    t_start = time.perf_counter()

    try:
        infos = socket.getaddrinfo(host, port, type=socket.SOCK_STREAM)
    except Exception as exc:
        return {
            "status": "error",
            "ip": host,
            "port": port,
            "latency_ms": None,
            "target": host,
            "notes": f"DNS resolution failed: {exc}",
            "region": "",
        }

    last_error = None
    for family, socktype, proto, canonname, sockaddr in infos:
        sock = None
        try:
            sock = socket.socket(family, socktype, proto)
            sock.settimeout(timeout)
            connect_start = time.perf_counter()
            sock.connect(sockaddr)
            latency_ms = (time.perf_counter() - connect_start) * 1000.0
            resolved_ip = sockaddr[0]
            status = "open"
            notes = "Connected"
            return {
                "status": status,
                "ip": resolved_ip,
                "port": port,
                "latency_ms": round(latency_ms, 2),
                "target": host,
                "notes": notes,
                "region": "",
            }
        except socket.timeout:
            last_error = "timeout"
        except ConnectionRefusedError:
            last_error = "refused"
        except OSError as exc:
            last_error = str(exc)
        finally:
            if sock is not None:
                try:
                    sock.close()
                except Exception:
                    pass

    return {
        "status": "timeout" if last_error == "timeout" else "closed",
        "ip": host,
        "port": port,
        "latency_ms": None,
        "target": host,
        "notes": last_error or "Connection failed",
        "region": "",
    }


def test_latency(host: str, port: int, timeout: float = DEFAULT_TIMEOUT) -> str:
    """
    Exposed function:
      - measure TCP latency to a target:port
      - resolve hostnames
      - return a JSON string
    """
    result = _connect_latency(host, port, timeout)
    return json.dumps(result, ensure_ascii=False)


def _filter_records(records: list[dict], region_filter: str) -> list[dict]:
    region_filter = (region_filter or "").strip().lower()
    if not region_filter:
        return records
    out = []
    for item in records:
        hay = " ".join(
            str(item.get(key, "") or "")
            for key in ("region", "notes", "target", "ip")
        ).lower()
        if region_filter in hay:
            out.append(item)
    return out


def filter_by_country(targets: Any, region_filter: str = "") -> str:
    """
    Exposed function:
      - filter any target/result list by a region/country string
      - works on region/note text tags so the Android app can remain offline
    """
    records = _json_loads_maybe(targets)
    if not isinstance(records, list):
        return json.dumps([], ensure_ascii=False)
    filtered = _filter_records(records, region_filter)
    return json.dumps(filtered, ensure_ascii=False)


def _results_to_text(results: list[dict]) -> str:
    lines = []
    for idx, r in enumerate(results, 1):
        lines.append(
            f"{idx:03d}. {r.get('status','')} | {r.get('ip','')}:{r.get('port','')} | "
            f"latency={r.get('latency_ms','--')} ms | notes={r.get('notes','')}"
        )
    return "\n".join(lines) + ("\n" if lines else "")


def export_results(results: Any, output_path: str, fmt: str = "csv") -> str:
    """
    Exposed function:
      - write CSV/TXT/JSON files
      - returns the saved path
    """
    records = _json_loads_maybe(results)
    if not isinstance(records, list):
        records = []

    path = Path(output_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    fmt = (fmt or "csv").lower().strip()

    if fmt == "json":
        payload = json.dumps(records, indent=2, ensure_ascii=False)
        path.write_text(payload, encoding="utf-8")
    elif fmt == "txt":
        path.write_text(_results_to_text(records), encoding="utf-8")
    else:
        with path.open("w", encoding="utf-8", newline="") as f:
            writer = csv.writer(f)
            writer.writerow(["status", "latency_ms", "ip", "port", "target", "region", "notes"])
            for r in records:
                writer.writerow([
                    r.get("status", ""),
                    r.get("latency_ms", ""),
                    r.get("ip", ""),
                    r.get("port", ""),
                    r.get("target", ""),
                    r.get("region", ""),
                    r.get("notes", ""),
                ])

    return str(path)


def scan_targets(
    targets: Any,
    ports: Any,
    region_filter: str = "",
    timeout: float = DEFAULT_TIMEOUT,
    concurrency: int = DEFAULT_CONCURRENCY,
) -> str:
    """
    Exposed function:
      - scans a batch of targets/ports
      - returns JSON with results and summary

    The Android app can call test_latency() repeatedly for progress-aware scans,
    while this helper stays available for direct use or future replacement.
    """
    records = _filter_records(_normalize_targets(targets), region_filter)
    port_list = _normalize_ports(ports)
    if not port_list:
        port_list = [443, 80]

    endpoints = []
    seen = set()
    for rec in records:
        host = rec["target"]
        if not host:
            continue
        if rec.get("explicit_port"):
            combo = (host, int(rec["explicit_port"]))
            if combo not in seen:
                seen.add(combo)
                endpoints.append((host, int(rec["explicit_port"]), rec))
        else:
            for port in port_list:
                combo = (host, port)
                if combo not in seen:
                    seen.add(combo)
                    endpoints.append((host, port, rec))

    results = []
    responsive = 0

    def worker(item):
        host, port, rec = item
        payload = json.loads(test_latency(host, port, timeout))
        payload["region"] = rec.get("region", "")
        payload["notes"] = " | ".join(
            x for x in [rec.get("notes", ""), payload.get("notes", "")] if x
        )
        return payload

    with concurrent.futures.ThreadPoolExecutor(max_workers=max(1, int(concurrency))) as executor:
        future_map = {executor.submit(worker, item): item for item in endpoints}
        for future in concurrent.futures.as_completed(future_map):
            try:
                item = future.result()
            except Exception as exc:
                item = {
                    "status": "error",
                    "latency_ms": None,
                    "ip": "",
                    "port": 0,
                    "target": "",
                    "region": "",
                    "notes": str(exc),
                }
            if item.get("status", "").lower() == "open":
                responsive += 1
            results.append(item)

    results.sort(key=lambda r: (
        0 if r.get("status", "").lower() == "open" else 1,
        r.get("latency_ms") if r.get("latency_ms") is not None else 10**9,
        str(r.get("ip", "")),
        int(r.get("port") or 0),
    ))

    payload = {
        "results": results,
        "summary": {
            "total": len(endpoints),
            "completed": len(results),
            "responsive": responsive,
        },
    }
    return json.dumps(payload, ensure_ascii=False)

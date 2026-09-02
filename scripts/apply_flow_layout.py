#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把 Warm-Flow 流程设计器导出的 JSON 坐标同步到数据库。

用法：
  python3 scripts/apply_flow_layout.py <json文件或目录> [--dry-run]

示例：
  python3 scripts/apply_flow_layout.py temp/采购申请审批_1.0.json
  python3 scripts/apply_flow_layout.py temp/ --dry-run
"""

import argparse
import json
import os
import subprocess
import sys

# 默认连接信息（与项目其他脚本保持一致）
MYSQL_HOST = "127.0.0.1"
MYSQL_PORT = "3306"
MYSQL_USER = "root"
MYSQL_PASS = "root"
MYSQL_DB = "ry-vue-6x"


def find_json_files(path: str):
    """收集所有待处理的 JSON 文件。"""
    if os.path.isfile(path):
        return [path]
    files = []
    for root, _, names in os.walk(path):
        for name in names:
            if name.endswith(".json"):
                files.append(os.path.join(root, name))
    return sorted(files)


def load_json(path: str):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def build_sql(definition_id: str, data: dict) -> str:
    lines = ["SET NAMES utf8mb4;"]
    lines.append(f"-- flow: {data['flowCode']} / {data['flowName']}, definition_id={definition_id}")

    lines.append("-- update flow_node coordinates")
    for node in data["nodeList"]:
        coord = node["coordinate"]
        nc = node["nodeCode"]
        lines.append(
            f"UPDATE flow_node SET coordinate = '{coord}' "
            f"WHERE definition_id = {definition_id} AND node_code = '{nc}' AND del_flag = '0';"
        )

    lines.append("-- update flow_skip coordinates")
    for node in data["nodeList"]:
        nc = node["nodeCode"]
        for skip in node.get("skipList", []):
            nnc = skip["nextNodeCode"]
            st = skip["skipType"]
            coord = skip["coordinate"]
            lines.append(
                f"UPDATE flow_skip SET coordinate = '{coord}' "
                f"WHERE definition_id = {definition_id} AND now_node_code = '{nc}' "
                f"AND next_node_code = '{nnc}' AND skip_type = '{st}' AND del_flag = '0';"
            )
    return "\n".join(lines) + "\n"


def exec_mysql(sql: str):
    cmd = [
        "docker", "exec", "-i", "mysql",
        "mysql",
        f"-u{MYSQL_USER}",
        f"-p{MYSQL_PASS}",
        f"-D{MYSQL_DB}",
        "--default-character-set=utf8mb4",
    ]
    result = subprocess.run(cmd, input=sql.encode("utf-8"), capture_output=True)
    if result.returncode != 0:
        print("[ERROR] MySQL 执行失败：")
        print(result.stderr.decode("utf-8", errors="replace"))
        sys.exit(1)
    print(result.stdout.decode("utf-8", errors="replace"))


def main():
    parser = argparse.ArgumentParser(description="同步 Warm-Flow 流程布局 JSON 到数据库")
    parser.add_argument("path", help="JSON 文件或目录")
    parser.add_argument("--dry-run", action="store_true", help="只打印 SQL，不执行")
    args = parser.parse_args()

    files = find_json_files(args.path)
    if not files:
        print(f"[WARN] 未找到 JSON 文件: {args.path}")
        sys.exit(0)

    for path in files:
        data = load_json(path)
        flow_code = data.get("flowCode")
        if not flow_code:
            print(f"[WARN] 跳过 {path}：缺少 flowCode")
            continue

        # 从数据库查询 definition_id（避免硬编码）
        lookup_sql = (
            f"SELECT id FROM flow_definition WHERE flow_code = '{flow_code}' "
            f"AND del_flag = '0' ORDER BY version DESC LIMIT 1;"
        )
        lookup_cmd = [
            "docker", "exec", "-i", "mysql",
            "mysql", f"-u{MYSQL_USER}", f"-p{MYSQL_PASS}",
            f"-D{MYSQL_DB}", "--skip-column-names", "-e", lookup_sql,
        ]
        result = subprocess.run(lookup_cmd, capture_output=True)
        if result.returncode != 0:
            print(f"[ERROR] 查询 {flow_code} 的 definition_id 失败：")
            print(result.stderr.decode("utf-8", errors="replace"))
            continue
        out = result.stdout.decode("utf-8", errors="replace").strip()
        if not out:
            print(f"[WARN] 跳过 {flow_code}：数据库中未找到对应流程定义")
            continue
        definition_id = out.split()[0]

        sql = build_sql(definition_id, data)
        print(f"\n[INFO] 处理：{path} -> {flow_code} (definition_id={definition_id})")
        if args.dry_run:
            print(sql)
        else:
            exec_mysql(sql)
            print("[OK] 已同步")


if __name__ == "__main__":
    main()

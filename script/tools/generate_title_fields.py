"""Generate the versioned title field catalog from PRD appendix C tables."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from docx import Document


TABLE_STEPS = {
    45: 1, 46: 2, 47: 3, 48: 4, 49: 1, 50: 2, 51: 4, 52: 5,
    53: 2, 54: 3, 55: 2, 56: 5, 57: 1, 58: 2, 59: 1, 60: 4,
}
REPEATABLE_OBJECTS = {
    "project_achievement", "peer_recommendation", "study_experience",
    "pre_title_achievement", "exam_record", "education_record",
}


def clean(value: str) -> str:
    return " ".join(value.replace("\n", " ").split())


def control_type(description: str) -> str:
    if "文件上传" in description:
        return "upload"
    if "日期" in description:
        return "date"
    if "数字" in description:
        return "number"
    if "多行" in description or "富文本" in description:
        return "textarea"
    if "下拉" in description or "单选" in description or "enum" in description:
        return "select"
    if "只读" in description:
        return "readonly"
    return "text"


def generate(prd: Path) -> list[dict]:
    document = Document(prd)
    fields: list[dict] = []
    for table_index, step in TABLE_STEPS.items():
        table = document.tables[table_index]
        for row in table.rows[1:]:
            cells = [clean(cell.text) for cell in row.cells]
            code, name, object_step, control, required, source, edit_rule, visibility, mapping = cells
            object_name = object_step.split("/")[0].strip()
            scope = "NON_MVP" if "非本期" in required or "非本期" in mapping else "P0"
            fields.append({
                "code": code,
                "name": name,
                "step": step,
                "object": object_name,
                "control": control_type(control),
                "format": control,
                "required": "必填" in required and scope == "P0",
                "requiredRule": required,
                "source": source,
                "editRule": edit_rule,
                "visibility": visibility,
                "mapping": mapping,
                "scope": scope,
                "visible": scope == "P0" and object_name != "review_ledger",
                "readOnly": "只读" in edit_rule or "系统生成" in source,
                "repeatable": object_name in REPEATABLE_OBJECTS,
            })
    codes = [field["code"] for field in fields]
    if len(fields) != 149 or len(set(codes)) != 149:
        raise RuntimeError(f"expected 149 unique fields, got {len(fields)} rows/{len(set(codes))} unique")
    return fields


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("prd", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    fields = generate(args.prd)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(fields, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"generated {len(fields)} fields -> {args.output}")


if __name__ == "__main__":
    main()

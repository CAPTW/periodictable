# Skill: Molar Mass Calculator Parser

- 토큰: ElementSymbol, Number, (, ), Dot(·)
- AST → element counts 합산 → 원자량 합으로 molar mass 계산
- 지원 범위는 단계적으로 확장(먼저 괄호 1단계)

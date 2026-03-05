# BACKLOG (Codex 자동 루프 친화)

> 원칙: 한 번에 1~3개 Task만 "완료 상태"로 끝내라.
> 완료 기준은 `agent/ACCEPTANCE.md` + `scripts/verify.sh` 통과.

---

## Epic A — 프로젝트/아키텍처 뼈대
- [ ] A1. Compose + Material3 기본 테마/네비게이션 스켈레톤 추가
- [ ] A2. 모듈 구조(선택): core/data, core/ui, feature/* (프로젝트 상황에 맞게)
- [ ] A3. Room + Repository 기본 배선(빈 DB라도)

## Epic B — 원소 데이터(오프라인)
- [ ] B1. 원소 데이터 소스(예: CSV/JSON) 번들 → Room import 파이프라인
- [ ] B2. Element 스키마에 필수 필드 포함 + 단위/결측치 정책
- [ ] B3. 샘플 10개 원소만이라도 우선 end-to-end (나중에 118개 확대)

## Epic C — Periodic Table UI(Interactive)
- [ ] C1. Table 화면: 줌/팬/탭으로 원소 선택 가능
- [ ] C2. 선택 원소 하이라이트 + group/period 가이드
- [ ] C3. 속성 컬러맵 모드(전기음성도/열전도 등 택 1부터)

## Epic D — Element Detail 화면
- [ ] D1. 원소 상세: 요약 + 필수 속성 표기(없으면 N/A)
- [ ] D2. 전자배치 표시 + 용어 링크
- [ ] D3. 핵특성(반감기/중성자 단면적(barn)) 섹션

## Epic E — 검색(필수)
- [ ] E1. 이름/기호/원자번호 검색
- [ ] E2. 속성 필터(범위 슬라이더 1~2개부터) + 정렬
- [ ] E3. 검색 결과 리스트에서 속성 시각화(칩/바)

## Epic F — 메모(필수)
- [ ] F1. 원소별 메모 CRUD(Room)
- [ ] F2. 원소 상세에 메모 탭/섹션
- [ ] F3. 메모 전체 검색(선택)

## Epic G — 몰 질량 계산기(필수)
- [ ] G1. 화학식 파서(괄호/중첩/수화물 점은 단계적으로)
- [ ] G2. 원소 심볼 입력 보조 UI(미니 테이블 또는 자동완성)
- [ ] G3. 결과: 몰 질량 + 원소별 기여도(선택)

## Epic H — 화학 용어집(필수)
- [ ] H1. 용어 데이터(JSON/MD) 번들 + 검색
- [ ] H2. 용어 상세(쉬운 설명 + 예시)
- [ ] H3. 원소 상세/속성에서 용어로 이동 링크

## Epic I — Stitch MCP 연동(권장)
- [ ] I1. Stitch로 핵심 5~6개 화면 생성(또는 가져오기)
- [ ] I2. 디자인 토큰 정리 → Compose Theme 반영
- [ ] I3. Screen 이미지/산출물 저장 위치 표준화(`ui/stitch/` 등)

agent/PRD.md, agent/BACKLOG.md, agent/ACCEPTANCE.md를 읽고,
아래 역할별로 병렬로 조사/설계/태스크 분해를 수행한 뒤 하나의 통합 계획으로 합쳐라.

역할:
- android_architect: 아키텍처/Compose 네비게이션/모듈 전략
- data_engineer: Element 데이터 모델/Room/검색 인덱스 전략
- ui_integrator: Stitch 기반 화면 목록/컴포넌트 구조/제스처 UX
- qa: 테스트/검증/수용기준/리스크

출력:
1) 통합 Implementation Plan (체크리스트)
2) 1~2일 스프린트 단위 Task Plan
3) `bash scripts/verify.sh`로 검증하는 절차

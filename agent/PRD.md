# Periodic Table On-Device Android App — PRD (Single Source of Truth)

## 목표
- 모바일 터치 학습에 최적화된 Interactive Periodic Table App
- Android에서 오프라인(온디바이스)으로 완벽하게 동작하도록 설계
- UI/UX는 Google Stitch MCP로 화면 설계(생성/가져오기) 워크플로를 포함

## Periodic Table 데이터(원소 상세에서 확인 가능해야 하는 항목)
- 물질의 상태(phase)
- 전자 배치(electron configuration)
- 몰(몰 질량 / molar mass 중심)
- 기화열(heat of vaporization)
- 비열(specific heat)
- 열팽창 계수(thermal expansion)
- 반감기(half-life)
- 중성자 단면적(neutron cross section)
- barn(단위 표시/용어 설명)
- 열전도 계수(thermal conductivity)
- 전기 음성도(electronegativity)
- 원자 반지름(atomic radius)

## 필수 인터랙션
1) 빠른 검색
- 원소 이름/기호/원자번호 검색
- 속성별 검색(필터 + 범위 + 정렬)

2) Table 학습 인터랙션
- 탭: 원소 상세 열기
- 핀치줌/팬: 표 탐색
- (선택) 롱프레스: 비교/핀/퀵툴팁

## 추가 기능(필수 포함)
- 원소별 메모(편집/저장/검색)
- 항목 목록(검색 결과 등)에서 속성 시각적 표시(막대/컬러/칩) + Table 컬러맵 모드
- 몰 질량 계산기(화학식 입력 편의: 원소 버튼/아래첨자/괄호/수화물 점 등)
- 화학 사전/용어집(쉽고 Interactive하게 설명, 속성/원소 화면에서 점프 가능)

## 비기능 요구사항
- Offline-first (네트워크 없이도 데이터/검색/계산기/메모/용어집 동작)
- Null-safe: 데이터 미상은 "N/A" 표시 정책 일관
- 접근성: 터치 타겟, TalkBack 라벨링
- 검증: `./scripts/verify.sh` 통과

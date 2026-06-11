# Phase 2E Review

## Blockers
- 없음. 모든 자동화 검증(validate_assets.py, unit tests, Kotlin 컴파일, assembleDebug, verify.ps1)이 정상 통과(PASS)하였으며, 기획 요건에 부합하게 구현되었습니다.

## Majors
- 없음.

## Minors
- 없음.

## Nits
1. **`validate_assets.py`의 용어 참조 무결성 검증 한계**:
   - `validate_assets.py`는 glossary.json의 스키마와 필수 텀의 존재만 확인하며, 개별 용어가 가진 `relatedTerms`나 `relatedElements`의 실제 존재 여부(참조 무결성)를 검증하지 않습니다.
   - 비록 `MoleculeGlossaryLinkResolverTest`의 `glossaryAsset_phase2ETermsReferenceExistingRelatedTerms`에서 신규 3개 텀에 대해 이 참조 무결성을 검증하고 있으나, 추후 Glossary 데이터 대규모 확장 시 실수로 존재하지 않는 용어 ID를 참조하게 될 리스크를 예방하기 위해 `validate_assets.py` 상에서 전체 용어의 참조 무결성을 교차 검증하도록 보완하는 것을 제안합니다.

2. **`MoleculeGlossaryLinkResolver`의 하드코딩 매핑**:
   - `MoleculeGlossaryLinkResolver`의 companion object 내에 화학식과 용어 ID 목록의 매핑(`termIdsByFormula`)이 하드코딩되어 있습니다.
   - MVP/현재 단계에서는 빠르고 안전한 구조나, 향후 미션 분자 종류가 추가되거나 레시피 데이터가 외부 JSON 등으로 분리될 경우 코드 수정이 불가피해집니다. 장기적으로는 이 매핑 구조 역시 메타데이터나 설정 데이터 형태로 외부화하는 개선이 권장됩니다.

3. **`MoleculeGameViewModel`의 중복 검사 로직**:
   - `MoleculeGameViewModel.kt`의 `GameEvent.OpenGlossary` 처리 시 `glossaryTerms.any { it.id == event.termId }` 체크를 수행합니다.
   - UI에 표시되는 `GlossaryLinkChip`은 이미 존재하는 용어 목록(`glossaryTerms`)을 기반으로 구성되었으므로, 해당 체크는 다소 중복적입니다. 로직상 무해하지만 불필요한 레이스 컨디션 우려나 코드 간소화 측면에서 Nit로 기록합니다.

## Recommended fixes before Phase 2F
- **추가 수정 불필요**: 식별된 Nit 항목들은 시스템 안정성 및 현재 요구사항 충족에 영향을 주지 않으므로, Phase 2F 개발 착수 전에 코드를 강제로 수정할 필요는 없습니다. 다음 단계로 즉시 진행을 권장합니다.

## Phase 2E 승인 가능 여부
- **승인 (APPROVED)**
- 빌드 검증 및 단위 테스트 커버리지가 우수하고, 기획 스펙(최대 3개 칩 노출, 우선순위 매핑 적용, H2O/CO2 '화학식' 생략, NaCl '염' 1순위 노출 등) 및 UI 가드, 접근성이 적절히 준수되었습니다.

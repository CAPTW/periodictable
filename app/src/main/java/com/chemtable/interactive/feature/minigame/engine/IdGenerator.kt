package com.chemtable.interactive.feature.minigame.engine

/**
 * 블록 id 발급기(단일 스레드 사용 가정). 병합/스폰으로 생기는 새 블록에 안정 id 를 부여한다.
 * 테스트 입력 블록은 보통 작은 id(1,2,3...)를 쓰므로 기본 시작값을 크게 두어 충돌을 피한다.
 */
class IdGenerator(start: Long = 1000L) {
    private var current = start
    fun next(): Long = current++
}

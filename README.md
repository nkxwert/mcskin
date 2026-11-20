# Self Skin (Forge 1.8.9)

나에게만 보이는 스킨 모드. 서버로 아무것도 보내지 않는 클라이언트 전용 외형 모드라서, 다른 플레이어에게는 원래 스킨이 그대로 보입니다.

## 빌드

### 방법 A: GitHub Actions (PC에 아무것도 설치할 필요 없음)
1. 이 폴더를 GitHub 저장소에 push
2. Actions 탭 → "Run Gradle Build" 완료 대기 (5~10분)
3. 실행 결과 아래 Artifacts에서 `mod-jar` 다운로드 → 안에 있는 `selfskin-1.1.0.jar`

### 방법 B: 로컬
JDK 17을 설치한 뒤:
```
./gradlew build        # Windows: gradlew.bat build
```
결과물: `build/libs/selfskin-1.1.0.jar`
(Java 8 툴체인은 Gradle이 자동으로 내려받습니다.)

참고: Loom 0.10의 `runClient`는 Gradle 8.8에서 동작하지 않습니다. 테스트는 빌드된 jar를 실제 게임의 `mods` 폴더에 넣어서 하세요.

## 사용법
- `K` 키 또는 `/selfskin`(`/myskin`) → 메뉴 열기
- `/selfskin on`, `/selfskin off` → 켜기/끄기

### 스킨
- 스킨 png는 `.minecraft/selfskin/skins/`에 넣고 [새로고침] (64x64 또는 64x32 크기). [폴더] 버튼으로 폴더 열기
- 닉네임 입력 후 [가져오기]를 누르면 해당 계정 스킨을 받아와 바로 적용
- 목록에서 누르면 적용 (`●` 적용 중, `▶` 선택됨)
- [▲ 위로] [▼ 아래로] → 선택한 스킨의 목록 순서 바꾸기
- 파일 탐색기에서 png 파일을 복사(Ctrl+C)한 뒤 메뉴에서 **Ctrl+V** → 스킨 폴더에 추가하고 바로 적용 (여러 개 가능, 같은 이름은 `이름 (2).png`로 저장)
  - 1.8.9 게임 창은 파일을 끌어다 놓기(드래그 앤 드롭)를 지원하지 않아서 붙여넣기 방식을 씁니다
- [이름] → 선택한 스킨 이름 바꾸기 (png 파일 이름도 함께 바뀜)
- [삭제] → 선택한 스킨 삭제 (확인 후 png 파일도 지움)
- [체형] 버튼으로 Steve(두꺼운 팔) / Alex(얇은 팔) 전환

### 갑옷·망토 (나에게만, 스킨 적용 여부와 상관없이 동작)
- 메뉴의 [갑옷·망토...] → 투구 / 흉갑 / 레깅스 / 부츠 / 머리 블록 / 망토를 각각 보임·숨김, [모두 보이기] [모두 숨기기]
- `/selfskin armor` → 갑옷 전체 전환, `/selfskin armor off` 전체 숨김, `on` 전체 보임
- `/selfskin armor helmet off` → 투구만 숨김 (`chestplate`, `leggings`, `boots`, `headblock`도 같은 방식, `on`/`off` 생략 시 전환)
- `/selfskin cape off` → 망토 숨김 (`on` 보임, 생략 시 전환)
- 머리 블록(호박, 머리, 블록 등)은 갑옷 전체 명령에 포함되지 않음

설정 파일: `.minecraft/config/selfskin.json`

### 언어 / Language
메뉴와 메시지는 게임 언어 설정을 따릅니다. 한국어로 설정하면 한국어, 그 외 언어는 영어로 표시됩니다.
Menus and messages follow the game's language setting: Korean (ko_KR) or English for every other language.
번역 파일: `src/main/resources/assets/selfskin/lang/` (`en_US.lang`, `ko_KR.lang`). 다른 언어는 같은 형식으로 파일을 추가하면 됩니다.

## 동작 원리
내 `NetworkPlayerInfo`의 `locationSkin` / `skinType` 필드를 매 프레임 내 텍스처로 덮어씁니다. 3인칭 모델, 1인칭 팔, 인벤토리 미리보기, 탭 리스트 머리 아이콘이 모두 이 값을 사용합니다. Mixin이나 코어모드 없이 Forge 이벤트와 리플렉션만 사용합니다.

## 최신 버전 (Minecraft 26.3) — Fabric / Forge
`modern/` 폴더에 26.3용 버전이 있습니다. 기능은 1.8.9 버전과 같고, 스킨 png를 **게임 창에 끌어다 놓으면** 바로 추가됩니다.

| 로더 | 폴더 | 필요 조건 |
|---|---|---|
| Fabric | `modern/fabric` | Fabric Loader 0.20.1+, Fabric API |
| Forge | `modern/forge` | Forge 66.1.9+ |

공통 코드는 `modern/common`에 있고, 두 로더가 같이 사용합니다. Java 25가 필요합니다.
```
cd modern/fabric && ./gradlew build   # → build/libs/selfskin-fabric-2.0.0+26.3.jar
cd modern/forge  && ./gradlew build   # → build/libs/selfskin-forge-2.0.0+26.3.jar
```
설정 파일과 스킨 폴더는 1.8.9 버전과 같은 위치·형식입니다 (`config/selfskin.json`, `selfskin/skins/`).
명령어도 같습니다. 다만 26.3에서는 명령어 부위 이름에 한글을 쓸 수 없어서 영어(`helmet` 등)만 지원합니다.

## 라이선스
[MIT](LICENSE) © 2026 nkxwert

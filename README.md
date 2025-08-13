# 회계마스터

영수증을 찍어 자동으로 인식하고 회계보고서를 작성해주는 애플리케이션입니다.

## 개요
지금 만드는 중이에요.  
테스트 계정:
ID: test / Email: test@test.test / PW: testtest

## 용어
- 레포지토리(Repository): 영수증들을 모아놓는 저장소

## 파일 구조

### `com.example.opensource`(java 파일들)

- `MainActivity`: 애플리케이션 실행 후 로그인 하고 난 메인화면
- `RepositoryListAdaptor`: `MainActivity`에 레포지토리 리스트 어뎁터
- `MyPageActivity`: 내 정보 화면

- **`auth` 패키지**: 로그인 관련 패키지
  - `LoginActivity`: 로그인 화면
  - `RegisterActivity`: 회원가입 화면

- **`camera` 패키지**: 카메라 찰칵 관련 패키지
  - 여기 부분은 소윤이가 코드 작성해서 모름

- **`file` 패키지**: 보고서 관련 패키지
  - `DataLoader`: `json` 또는 `ReceiptManager`객체로부터 `ReportData`를 만드는 유틸
  - `FileGeneratorActivity`: 파일을 생성하는 화면
  - `ReportData`: 보고서 한 행에 관한 내용
  - `TemplateAdapter`: `FileGeneratorActivity`에 표시되는 양식 리스트 어뎁터
  - `TemplateItem`: `TemplateAdapter`에 표시되는 양식 데이터
  - `WordReportGenerator`: 보고서 생성 유틸

- **`firebase` 패키지**: 파이어베이스와 관련된 패키지
  - `FileStorage`: 파일을 파이어베이스에 저장할 수 있음

- **`receipt` 패키지**: 영수증 관련 패키지
  - `ReceiptManager`: 영수증들 관리하는 매니저
  - `ReceiptParser`: [ `Json` <-> `Receipt` ] 서로 변환해주는 유틸
  - `entity.Receipt`: 영수증 데이터
  - `entity.ReceiptItem`: 영수증 하위 항목(구매 항목) 데이터

- **`repository` 패키지**: 레포지토리 관련 패키지
  - `RepositoryActivity`: 레포지토리 화면(영수증 관리, 추가, 삭제 할 수 있는 화면)
  - `RepositoryData`: 레포지토리 데이터, 메인화면에서 `RepositoryListAdaptor`로 리스트 띄울 때 사용함
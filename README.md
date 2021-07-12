# OCR_PJS

## 작성자 : 박진수
## 개발 환경 : Android Studio
## 사용언어 : kotlin

### keyword
[ google firebase ]  
- login  
- clound function  
- storage  
- realtime database  
- cloud vision api  
- cloud messaging
  
[ external server db ]  
- oracle
- tomcat
- jsp

[ android local ]  
- kotlin coroutine  
- sqlite  
- room  
- okhttp3  

### 프로그램 실행 흐름
1. 어플 진입
2. 권한 확인 -> 권한이 없다면 권한을 요청하는 UI 실행
3. 권한 확인 완료
4. (OCR 기능 이용을 위한) 구글 로그인 확인 -> 로그인되지 않았다면 로그인 UI 실행
5. 구글 로그인 완료
6. 사진을 얻기위한 카메라 실행
7. 사진 촬영 완료 시 사진을 화면에 표시하고 OCR 시도  
  1. 텍스트 인식 성공한다면  
      1. 인식된 텍스트 화면 하단 텍스트뷰에 표시  
      2. 비동기로 사진과 텍스트를 DB에 저장 시도  
      3. 외부 DB 저장 시도 성공시 완료  
      4. 클라우드 DB 저장 시도 성공시 완료  
      5. 내장 DB 저장  
  2. 텍스트 인식 실패한다면  
      1. 화면 하단 텍스트뷰에 인식 실패 표시
8. 실행 흐름 완료
    
### 세부 설명
- Activity 는 MainActivity 하나만을 갖고 있습니다
- 카메라는 직접 구현하지 않고 Intent 로 카메라 기능을 요청하여 사용합니다
- Static.kt 에 범용적으로 사용할 함수와 상수 등을 정의합니다
- AppSharedPreferences(싱글톤)을 통해 SharedPreferences 를 사용합니다
- DBManager(싱글톤)를 통해 외부DB, 클라우드DB, 내장DB의 순으로 저장을 시도합니다
- 외부 서버는 개인PC를 사용합니다
- 클라우드 서버는 파이어베이스를 사용합니다
- localDB 패키지에는 Room 관련한 클래스들(Database, Entity, DAO 등)이 포함됩니다

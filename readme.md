# EyesON : 졸음운전 방지 솔루션 

[![Build Status](https://travis-ci.org/inerplat/EyesON.svg?branch=master)](https://travis-ci.org/inerplat/EyesON)
[![GitHub](https://img.shields.io/github/license/inerplat/EyesON)](https://github.com/inerplat/EyesON/blob/master/LICENSE)
[![GitHub release (latest by date including pre-releases)](https://img.shields.io/github/v/release/inerplat/EyesON?include_prereleases)](https://github.com/inerplat/EyesON/releases)


[![GitHub issues](https://img.shields.io/github/issues/inerplat/EyesON?color=red)](https://github.com/inerplat/EyesON/issues)
[![GitHub closed issues](https://img.shields.io/github/issues-closed/inerplat/EyesON?color=green)](https://github.com/inerplat/EyesON/issues?q=is%3Aissue+is%3Aclosed)
[![GitHub pull requests](https://img.shields.io/github/issues-pr/inerplat/EyesON)](https://github.com/inerplat/EyesON/pulls)
[![GitHub closed pull requests](https://img.shields.io/github/issues-pr-closed/inerplat/EyesON)](https://github.com/inerplat/EyesON/pulls?q=is%3Apr+is%3Aclosed)

[![GitHub project](https://img.shields.io/badge/Project-Khanban-ff509f?style=for-the-badge)](https://github.com/inerplat/EyesON/projects/1)


사용자의 얼굴을 분석해 졸음 여부를 파악하고, 임베디드 단말기와 연결하여 경고를 주는 솔루션 입니다.

 - 얼굴 인식을 통한 졸음 감지 : 학습된 인공지능 모델을 이용해 사용자의 눈을 파악해 졸음 패턴을 분석합니다.

 - 사용자의 졸음을 깨우는 피드백 : 모터 및 부저를 이용해 졸고 있는 사용자에게 경고를 줍니다.

 - 사용자 로그 데이터 분석 : Google Analytics를 이용해 사용자의 로그 데이터를 분석합니다.

    <img src="https://github.com/inerplat/EyesON/blob/master/docs/image/function.jpg?raw=true">


## 1. 개발 목적

현대 도시에서 살아가기 위해 꼭 필요한 자동차, 현재 한국에 등록된 자동차는 2천 2백만대를 넘어섰고 가구당 0.9대를 보유하고 있을 정도로 많이 그리고 자주 사용됩니다.

이런 자동차에도 사고라는 위험요소가 존재하는데, 운전자가 일으킬 수 있는 대표적인 사고로 음주운전과 졸음운전을 뽑을 수 있습니다.
음주운전은 술을 마신사람이 일으키는 사고로 최근에는 윤창호법으로 강력히 처벌을 할 수 있지만, 졸음운전은 음주운전과는 다르게 누구에게나 갑작스럽게 찾아올수 있는 위험 요소입니다.
졸음운전을 예방하기 위해 음악을 듣거나 창문을 여는 등 여러가지 방법이 제시되고 있지만, 근본적으로 졸기 시작한 사람을 깨우기는 어려운 실태입니다.
   
   <img src="https://github.com/inerplat/EyesON/blob/master/docs/image/introduce.jpg?raw=true">


따라서 당장 운전을 하면서 졸고있는 사람을 스마트폰의 소리와 쿠션의 진동으로 깨워주는 졸음 운전 방지 솔루션을 기획하게 되었습니다.

## 2. 개발 환경

### Project Manage
 - Github 
    - [Khanban](https://github.com/inerplat/EyesON/projects/1)
    - [Issue](https://github.com/inerplat/EyesON/issues?utf8=%E2%9C%93&q=is%3Aissue) 
    - [PR](https://github.com/inerplat/EyesON/pulls?utf8=%E2%9C%93&q=is%3Apr)
 - Travis
    - [CI](https://travis-ci.org/inerplat/EyesON)

### Android
 - IDE 
    - Android Studio 3.5.1 
    - VSCode 1.39.2
 - JRE 
    - 1.8.0_202-release-1483-b03 amd64
 - JVM 
    - OpenJDK 64-Bit Server VM by JetBrains s.r.o
 - OS
    - Windows 7 6.1
 - SDK
    - Compile-Version : 29
    - Min-Version : 21
 - Tools 
    - Firebase-MLKit
    - Google Analytics

### Arduino
 - IDE
	- ARDUINO 1.8.10
 - Compiler
	- gcc Version 5.4.0
 - Uploader
	- avrdude Version 6.3
 - Board
	- arduino UNO(ATMEGA328P)


## 3. 개발 내용

### Block Diagram

<img src = "https://github.com/inerplat/EyesON/blob/master/docs/image/block.jpg?raw=true" />

### Arduino Schematic

<img src = "https://github.com/inerplat/EyesON/blob/master/docs/image/arduino.jpg?raw=true" />

### 졸음 감지 알고리즘

 - [Drowsy Driver Detection System Using Eye Blink(2018)](https://www.researchgate.net/publication/251970873_Drowsy_driver_detection_system_using_eye_blink_patterns)

 - [The spontaneous eye-blink as sleepiness indicator in patients with obstructive sleep apnoea syndrome-a pilot study (2005)](https://www.researchgate.net/publication/251970873_Drowsy_driver_detection_system_using_eye_blink_patterns)

### 통신 프로토콜

   <img src = "https://github.com/inerplat/EyesON/blob/master/docs/image/protocol.jpg?raw=true">

## 4. 사업 목표

졸음운전을 예방하기 위한 시장은 이미 몇가지 제품이 있는 초기단계의 시장입니다.
Eyes ON은 공익적인 성향을 가지고있는 제품으로 단순히 상품판매만으로 수익을 창출하기에는 어려움이 있습니다.

원할한 시장 경쟁력을 확보하기 위해 기능을 앱과 디바이스로 분할하여 B2C모델을 구상하였습니다.
   - 어플리케이션을 통한 광고 수익
   - 임베디드 디바이스를 통한 판매 수익

      <img src="https://github.com/inerplat/EyesON/blob/master/docs/image/b2c.jpg?raw=true" />


현재 시장에도 카메라로 얼굴을 인식하는 임베디드 제품이 존재하여 해당 재품과 경쟁을 하기 위해 스마트폰 앱을 무료배포할 계획입니다.
소비자들은 별도의 임베디드 제품의 구입 없이도 Eyes ON기능을 일부 이용할 수 있기 때문에 현 시장의 제품과 충분한 경쟁이 되고, 
광고수익을 창출할 수 있기 때문에 소비자가 별도의 디바이스를 구입하지 않더라도 큰 타격이 되지 않습니다.

현재 구상한 임베디드 디바이스는 진동이 나는 쿠션으로 한정되어 있지만, 졸음을 깨울 수 있는 아이템(손목밴드 등)을 추가로 제작할 예정이며,
이를 어플리케이션과 연동하여 이용할 수 있도록 판매할 계획입니다.

 또한 사업성장 이후 독자적인 플랫폼과 얼굴 인식 모델을 구축하면 다음과 같은 B2B모델도 고려해 볼 수 있습니다.
   - 내비게이션 업체의 제품들과 연계하여 내비게이션 및 차량 고급화
   - 얼굴 및 졸음 인식 API 판매 수익

      <img src="https://github.com/inerplat/EyesON/blob/master/docs/image/b2b.jpg?raw=true" />


## 5. 세부 작동 이미지

### 얼굴 인식 및 윤곽선 표시 / 해제 기능
<img src ="https://github.com/inerplat/EyesON/blob/master/docs/image/contour.gif?raw=true" width="50%" />

### 일반적인 눈 깜빡임 : 변화 없음
<img src = "https://github.com/inerplat/EyesON/blob/master/docs/image/nomal-close.gif?raw=true" width="50%" />

### 졸음이 감지된 눈 : 빨간색 사각형
<img src = "https://github.com/inerplat/EyesON/blob/master/docs/image/doze-close.gif?raw=true" width="50%" />

### 웃는 얼굴(알람 종료용 트리거) : 파란색 사각형
<img src = "https://github.com/inerplat/EyesON/blob/master/docs/image/smile.gif?raw=true" width="50%" />

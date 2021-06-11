<h1 align="center">Likes Predictor / Mobile</h1>
<h2>О разделе</h2>
В данном разделе находится код мобильного приложения.

<h2>Структура проекта</h2>

    .
    ├─ app
    │  ├─ libs
    │  ├─ src
    │  │  ├─ androidTest
    │  │  │  └─ java    
    │  │  └─ main
    │  │     ├─ java
    │  │     │  └─ java/com/example/likespredictor
    │  │     │     ├─ MainActivity.java                     # Класс MainActivity.java отвечает за авторизацию пользователя в Instagram
    │  │     │     └─ CameraActivity.java                   # Первоначальные настройки камеры, работа пользователя с камерой, применение моделей к кадру и вывод результата
    │  │     ├─ asserts                                     # Папка, содержащая обученные модели
    │  │     │  ├─ aestetics.pt
    │  │     │  ├─ regression_v2.pt
    │  │     │  └─ resnet_v4.pt
    │  │     ├─ res
    │  │     │  └─ ...
    │  │     └─ AndroidManifest.xml
    │  ├─ build.gradle
    │  └─ proguard-rules.pro
    ├─ .gitignore
    ├─ gradlew.bat
    ├─ gradle.properties
    ├─ build.gradle
    ├─ gradlew
    ├─ settings.gradle
    └─ README.md
  

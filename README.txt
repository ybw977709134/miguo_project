onemeter.co  
一米家校 Android 客户端


开发环境：

* Android Studio

系统需求：

* 操作系统不限
* 需要安装的软件：
    - JDK
    - Maven
    - Gradle
    - Android SDK 
        # Platform 19
        # Build Tools 21.1.2
        # Extras / Android support Repository
* 需要定义的环境变量：
    - HOME=当前用户的HOME路径 ( *nix 上 HOME=~ ；Windows 上 HOME=%HOMEDRIVE%%HOMEPATH% )
    - JAVA_HOME=JDK的根目录
    - ANDROID_HOME=Android SDK的根目录
* 需要安装一些 jar/aar 到本地 maven 仓库，详见 doc/build_with_gradle.txt


开发环境配置好之后，在命令行中应该可以通过以下命令完成编译：

    gradle aDev01Debug

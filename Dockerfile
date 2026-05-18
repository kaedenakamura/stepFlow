# 1. ベースとなるOSとJavaの環境（既製品）を用意する
#FROM openjdk:17-jdk-slim

# 2. パソコン（コンテナ）の中に、アプリを配置するフォルダを作る
#WORKDIR /app

# 3. 楓さんがEclipseでビルドしたJavaの実行ファイル（.jar）を、コンテナの中にコピーする
#COPY target/stepflow-0.0.1-SNAPSHOT.jar app.jar

# 4. このパソコンが起動した瞬間に、Javaアプリを動かすコマンドを実行する
#ENTRYPOINT ["java", "-jar", "app.jar"]
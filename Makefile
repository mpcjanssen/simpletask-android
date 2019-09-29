.PHONY: artifacts

artifacts:
	./gradlew build
	find . -name "*.apk" -exec scp {} ssh.mpcjanssen.nl:/var/www/mpcjanssen/html/artifacts \;

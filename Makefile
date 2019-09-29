.PHONY: artifacts

artifacts:
	./gradlew assemble
	find . -name "*.apk" -exec scp {} ssh.mpcjanssen.nl:/var/www/mpcjanssen/html/artifacts/saf \;

.PHONY: artifacts assemble

assemble:
	./gradlew assemble

artifacts: assemble
	find . -name "*.apk" -exec scp {} ssh.mpcjanssen.nl:/var/www/mpcjanssen/public/artifacts \;


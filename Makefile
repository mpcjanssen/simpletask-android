.PHONY: artifacts assemble

assemble:
	./gradlew assemble

artifacts: assemble
	(cd app/build/outputs/apk && scp -r * ssh.mpcjanssen.nl:/var/www/mpcjanssen/public/artifacts)

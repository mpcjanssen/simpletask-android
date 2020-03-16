.PHONY: artifacts assemble upload

GITNAME=$(shell ./gradlew app:androidGitVersionName --quiet)
assemble:
	./gradlew clean assembleNextcloud assembleDokuwiki assembleCloudless assembleDropbox

upload:
	(cd app/build/outputs && scp -r apk ssh.mpcjanssen.nl:/var/www/mpcjanssen/public/artifacts/${GITNAME})

artifacts: assemble upload

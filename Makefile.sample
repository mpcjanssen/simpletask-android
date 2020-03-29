.PHONY: artifacts assemble upload

GRADLE:=./gradlew
GITNAME=$(shell ${GRADLE} app:androidGitVersionName --quiet)
artifacts: assemble upload

assemble:
	${GRADLE} clean assembleNextcloud assembleDokuwiki assembleCloudless assembleDropbox assembleWebdav

upload:
	(cd app/build/outputs && scp -r apk ssh.mpcjanssen.nl:/var/www/mpcjanssen/artifacts/${GITNAME})



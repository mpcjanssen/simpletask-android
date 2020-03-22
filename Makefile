.PHONY: artifacts assemble upload

GRADLE:=./gradlew
GITNAME=$(shell ${GRADLE} app:androidGitVersionName --quiet)
assemble:
	${GRADLE} clean assembleNextcloud assembleDokuwiki assembleCloudless assembleDropbox assembleWebdav

upload:
	(cd app/build/outputs && scp -r apk ssh.mpcjanssen.nl:/var/www/mpcjanssen/public/artifacts/${GITNAME})

artifacts: assemble upload

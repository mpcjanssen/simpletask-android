git fast-export bd5fa57c242131100f103b73ce349007d6c188a8..master | fossil import --git .fossil -i
fossil uv add build/outputs/apk/*
fossil sync -u

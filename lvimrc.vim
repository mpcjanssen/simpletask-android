" vimrc in project root

if exists ("s:loaded")
    finish
endif

let s:loaded=1
" Absolute path of script file:
let s:path = expand('<sfile>:p:h')
let g:project_root = s:path 

set tags=tags;/

set makeprg=gradlew\ --no-color


function! AndroidMonitor()
    let l:filename = g:project_root."/local.properties"
    execute "vimgrep /\\v^sdk\\.dir=(.*)\s*$/j ".l:filename
    for i in getqflist()
        echom "found ". i.text
        let l:cmd = split(i.text,"=")[1]
        execute ":Start! " . l:cmd . "\\tools\\monitor.bat"
        break
    endfor
endfunction

command! InstallFreeRelease :Make! "installFreeRelease"
command! AndroidMonitor :call AndroidMonitor()

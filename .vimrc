set tags=tags;/

let s:script_path = expand('<sfile>:p:h') . "/" 
let g:ctrlp_custom_ignore = {
  \ 'dir':  '\vbuild$',
  \ }

nnoremap <leader>f :call FuzzyFindFromHere()<CR>

function! FuzzyFindFromHere()
    execute "CtrlP ".s:script_path
endfunction

function! RunGradleTask(task)
     execute ":cd ".s:script_path
     execute ":!start cmd /c gradlew ".a:task
endfunction

command! InstallFreeRelease :call RunGradleTask("installFreeRelease")

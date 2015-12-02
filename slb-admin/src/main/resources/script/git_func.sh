function commit_all_changes {
    cd $target_dir
    local change_files=`git status --short | wc -l`
    if [ $change_files -gt 0 ];then
        log "git committing $change_files files"
        git add -A
        git commit -m "$comment"
    else
        log "no file changed, no commit necessary"
    fi
    cd - >/dev/null
}

function tag_and_push {
    cd $target_dir
    local change_files=`git status --short | wc -l`
    if [ $change_files -gt 0 ];then
        log_error "There's unstaged changes in $target_dir, exit code is $?"; 
        exit 1; 
    else
        log "adding git tag $tag for folder $target_dir"
        git tag -a $tag -m"$comment"
        #git push
        #git push --tags
    fi
    cd - >/dev/null
}

function push {
    cd $target_dir
    local change_files=`git status --short | wc -l`
    if [ $change_files -gt 0 ];then
        log_error "There's unstaged changes in $target_dir, exit code is $?"; 
        exit 1; 
    else
        log "git pushing for folder $target_dir"
        git push
    fi
    cd - >/dev/null
}

function clone {
    if [[  -d "$target_dir" ]]; then
        rm -rf "$target_dir"
        log "$target_dir cleared"
    fi
    
    mkdir -p "$target_dir"
    log "$target_dir created"

    cd "$target_dir"
    
    log "cloning $git_url to $target_dir"
    git clone $git_url $target_dir
    if [ "$tag" ]; then
        log "checking out to tag $tag"
        git checkout $tag
    fi

    cd - >/dev/null
}

function rollback {
    log "rolling back $target_dir"
    
    cd $target_dir/
    git reset --hard
    cd - >/dev/null
}
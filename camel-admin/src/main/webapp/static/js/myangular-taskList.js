var module = angular.module('MyApp', ['ngResource']);

module.config(function ($locationProvider, $resourceProvider) {
    // configure html5 to get links working on jsfiddle
    $locationProvider.html5Mode(true);
});

module.directive('ngEnter', function () {
    return function (scope, element, attrs) {
        element.bind("keydown keypress", function (event) {
            if (event.which === 13) {
                scope.$apply(function () {
                    scope.$eval(attrs.ngEnter);
                });
                event.preventDefault();
            }
        });
    };
});

module.controller('TaskListController', function ($scope, $resource, $http) {
    $scope.delTaskId = -1;
    $scope.newTask = {};
    $scope.newTask.taskName = '未命名任务';
    $scope.vsList = [];
    $scope.groups = {};

    $scope.vs2Tags = {};// vs和tag的cache
    $scope.getTags = function (vs) {
        var vsName = vs.name;
        if (vsName == null || vsName.trim() == ""
            || $scope.vs2Tags[vsName] != null) {
            return;
        }
        $http({
            method: 'GET',
            url: window.contextpath + '/console/vs/' + vsName + '/tag/list'
        }).success(function (data, status, headers, config) {
            $scope.vs2Tags[vsName] = data;
            if (data.length > 0) {
                vs.selectedTag = data[0];
            }
        }).error(function (data, status, headers, config) {
            //app.appError("响应错误", data);
        });
    }

    $http({
        method: 'GET',
        url: window.contextpath + '/console/vs/list'
    }).success(function (data, status, headers, config) {
        $scope.vsList = data;
        // 将所有vs分group
        $.each($scope.vsList, function (i, vs) {
            var groupName = vs.group;
            if (!groupName) {
                groupName = 'default';
            }
            var group = $scope.groups[groupName];
            if (!group) {
                group = new Object();
                group.name = groupName;
                group.vsList = [];
                $scope.groups[groupName] = group;
            }
            group.vsList.push(vs);
        });
        // 初始化table
        setTimeout(function () {
            $.each($scope.groups, function (groupName, group) {
                console.log(groupName);
                $('#groupTable_' + groupName).dataTable({
                    "bPaginate": false,
                    "bLengthChange": false,
                    "bInfo": false,
                    "aoColumns": [{
                        "bSortable": false
                    }, null, {
                        "bSortable": false
                    }],
                    "aaSorting": [[1, 'asc']]
                });

            });
        }, 500);

    }).error(function (data, status, headers, config) {
//		app.appError("响应错误", data);
    });

    $scope.check = function (vs) {
        if (vs.selected) {
            vs.selected = false;
        } else {
            vs.selected = true;
            $scope.getTags(vs)
        }
    }
    $scope.checkAll = function (group) {
        $.each(group.vsList, function (i, vs) {
            vs.selected = true;
            $scope.getTags(vs);
        });
        group.checkAll = true;
    }
    $scope.uncheckAll = function (group) {
        $.each(group.vsList, function (i, vs) {
            vs.selected = false;
        });
        group.checkAll = false;
    }
    $scope.openRemoveTaskModal = function (taskId) {
        $scope.delTaskId = taskId;
        $('#removeTaskAlertDiv').html('');
        $('#removeTaskModal').modal('show');
    }
    // 删除
    $scope.removeTask = function () {
        var param = new Object();
        param.deployTaskId = $scope.delTaskId;
        $http({
            method: 'POST',
            data: $.param(param),
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            url: window.contextpath + '/console/deploy/task/del'
        }).success(
            function (data, status, headers, config) {
                if (data.errorCode == 0) {
                    window.location = window.contextpath + "/console/deploy";
                } else {
                    app.alertError("删除失败: " + data.errorMessage,
                        "removeTaskAlertDiv");
                }
            }).error(function (data, status, headers, config) {
//			app.appError("响应错误", data);
            });
    };

    $scope.addTask = function () {
        // 遍历一次，将已经勾选的，加到newTask.selectedVsAndTags
        $scope.newTask.selectedVsAndTags = [];
        $.each($scope.vsList, function (i, vs) {
            if (vs.selected == true) {
                $scope.newTask.selectedVsAndTags.push({
                    "vsName": vs.name,
                    "tag": vs.selectedTag
                });
            }
        });

        $http({
            method: 'POST',
            data: $scope.newTask,
            url: window.contextpath + '/console/deploy/task/add'
        }).success(
            function (data, status, headers, config) {
                if (data.errorCode == 0) {
                    app.alertSuccess("保存成功！ 即将刷新页面...", "addTaskAlertDiv");
                    vsChanged = false;// 保存成功，修改标识重置
                    setTimeout(function () {
                        window.location = window.contextpath
                            + '/console/deploy/task/' + data.taskId;
                    }, 700);
                } else {
                    app.alertError("保存失败: " + data.errorMessage,
                        "addTaskAlertDiv");
                }
            }).error(function (data, status, headers, config) {
//			app.appError("响应错误", data);
            });
    }
    // 如果地址栏含有“#showInfluencing:vs,vs”，则显示
    var hash = '' + window.location.hash;
    if (app.startWith(hash, '#showInfluencing:')) {
        // 解析url
        hash = hash.substring(17);
        var vsNamesStr = hash;
        var tagIdsStr = null;
        if (hash.indexOf('&') > 0) {
            var hashSplit = hash.split('&');
            vsNamesStr = hashSplit[0];
            tagIdsStr = hashSplit[1];
        }
        var vsNames = vsNamesStr.split(',');
        var tags = null;
        if (tagIdsStr != null) {
            tags = tagIdsStr.split(',');
        }
        // 设置$scope.newTask
        $scope.newTask.taskName = "修改pool后的批量发布：" + vsNamesStr;
        $scope.newTask.selectedVsAndTags = [];
        setTimeout(function () {
            $.each(vsNames, function (i, vsName) {
                $.each($scope.vsList, function (i, vs) {
                    if (vs.name == vsName) {
                        console.log(vs.name);
                        vs.selected = true;
                        $scope.getTags(vs);
                        vs.selectedTag = (tags != null) ? tags[i] : "";
                    }
                });
            });
        }, 1000);
        // 展开创建页面
        $scope.creating = true;
    }

});

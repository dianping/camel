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

module.directive('booleanValue', function () {
    return function (scope, elm, attr) {
        attr.$set('value', attr.booleanValue === 'true');
    };
});

module
    .controller(
    'TaskController',
    function ($scope, $resource, $http) {
        $scope.task = null;
        $scope.canUpdate = false;

        // check and collapse
        $scope.allCollapse = false;
        $scope.allChecked = false;

        // log
        $scope.currentLogView = {};
        $scope.currentLogView.showTask = true;
        $scope.currentLogView.agentIp = null;

        $scope.getTask = function (taskId) {
            // 获取task
            $http(
                {
                    method: 'GET',
                    url: window.contextpath
                    + '/console/apideploy/task/'
                    + taskId + '/get'
                }).success(
                function (data, status, headers, config) {
                    if (data.errorCode == 0) {
                        $scope.task = data.task;
                        $scope.buildVsTag($scope.task);
                        // 展现出来
                        $('#TaskController > div.main-content')
                            .show();

                        // 开始ajax论询获取task的状态
                        $scope.needGetStatus = true;
                    } else {
                        app.alertError("获取失败: "
                            + data.errorMessage);
                    }
                }).error(
                function (data, status, headers, config) {
                    app.appError("响应错误", data);
                });
        };
        $scope.buildVsTag = function (task) {
            $.each(task.deployAgentBos, function (i, deployAgentBo) {
                if (deployAgentBo.deployAgent.vsNames
                    && deployAgentBo.deployAgent.vsTags) {
                    deployAgentBo.vsAndTags = [];
                    var vsNames = deployAgentBo.deployAgent.vsNames
                        .split(',');
                    var vsTags = deployAgentBo.deployAgent.vsTags
                        .split(',');
                    $.each(vsNames, function (i, vsName) {
                        var vsAndTag = new Object();
                        vsAndTag.vsName = vsName;
                        vsAndTag.vsTag = vsTags[i];
                        deployAgentBo.vsAndTags.push(vsAndTag);
                    });
                }
            });
        }
        $scope.batchUnCollapse = function () {
            $(".panel-collapse").collapse('show');
            $(".accordion-toggle").removeClass('collapsed');
            $scope.allCollapse = false;
        }
        $scope.batchCollapse = function () {
            $(".panel-collapse").collapse('hide');
            $(".accordion-toggle").addClass('collapsed');
            $scope.allCollapse = true;
        }
        $scope.replaceDot2Underline = function (str) {
            return str.replace(/\./g, '_');
        }
        $scope.startTask = function () {
            $scope.needGetStatus = true;
            $http(
                {
                    method: 'GET',
                    url: window.contextpath
                    + '/console/apideploy/task/'
                    + $scope.task.task.id + '/start'
                }).success(
                function (data, status, headers, config) {
                    if (data.errorCode == 0) {
                        $scope.needGetStatus = true;
                    } else {
                        app.alertError("操作失败: "
                            + data.errorMessage);
                    }
                }).error(
                function (data, status, headers, config) {
                    app.appError("响应错误", data);
                });
        }
        $scope.stopTask = function () {
            $http(
                {
                    method: 'GET',
                    url: window.contextpath
                    + '/console/apideploy/task/'
                    + $scope.task.task.id + '/stop'
                }).success(
                function (data, status, headers, config) {
                    if (data.errorCode == 0) {
                    } else {
                        app.alertError("操作失败: "
                            + data.errorMessage);
                    }
                }).error(
                function (data, status, headers, config) {
                    app.appError("响应错误", data);
                });
        }
        $scope.statusConsole = function () {
            $http(
                {
                    method: 'GET',
                    url: window.contextpath
                    + '/console/apideploy/task/'
                    + $scope.task.task.id + '/status'
                })
                .success(
                function (data, status, headers, config) {
                    if (data.errorCode == 0) {
                        $scope.task = data.task;
                        $scope.buildVsTag($scope.task);

                        // 如果有agent处于'PROCESSING'状态，则console显示它。
                        $
                            .each(
                            $scope.task.deployAgentBos,
                            function (i,
                                      deployAgentBo) {
                                if (deployAgentBo.deployAgent.status == 'PROCESSING') {
                                    // 显示运行的图标
                                    deployAgentBo.isRunning = true;
                                }
                            });

                        $scope.showLog();

                        // 状态是成功，或停止，则不再获取状态(延迟停止，以免启动时状态还是STOP，造成错误停止)
                        if ($scope.task.task.status == 'SUCCESS'
                            || $scope.task.task.stateAction == 'STOP') {
                            setTimeout(
                                function () {
                                    if ($scope.task.task.status == 'SUCCESS'
                                        || $scope.task.task.stateAction == 'STOP') {
                                        $scope.needGetStatus = false;
                                    }
                                }, 10000);
                        }
                    } else {
                        app.alertError("获取失败: "
                            + data.errorMessage);
                    }
                })
                .error(function (data, status, headers, config) {
                    $scope.needGetStatus = false;// 发生网络错误
                });
        };
        $scope.showTaskLog = function () {
            $scope.currentLogView.showTask = true;
            $scope.currentLogView.agentIp = null;
            $scope.showLog();
        }
        $scope.showAgentLog = function (deployAgentBo) {
            if (deployAgentBo) {
                $scope.currentLogView.showTask = false;
                $scope.currentLogView.agentIp = deployAgentBo.deployAgent.ipAddress;
                $scope.showLog();
            }
        }
        $scope.unescape = function (str) {
            var r = /\\u([\d\w]{4})/gi;
            str = str.replace(r, function (match, grp) {
                return String.fromCharCode(parseInt(grp, 16));
            });
            return unescape(str);
        }
        $scope.showLog = function () {
            if ($scope.currentLogView.showTask) {
                $('#console')
                    .text(
                    $scope.task.task.summaryLog != null ? $scope.task.task.summaryLog
                        : "");
            } else {
                $
                    .each(
                    $scope.task.deployAgentBos,
                    function (i, deployAgentBo) {
                        var breakFor = true;
                        if ($scope.currentLogView.agentIp == deployAgentBo.deployAgent.ipAddress) {

                            $('#console')
                                .text(
                                $scope.unescape(deployAgentBo.deployAgent.rawLog != null ? deployAgentBo.deployAgent.rawLog
                                    : ""));
                            breakFor = false;
                        }
                        return breakFor;
                    });
            }
        }
        setInterval(function () {
            if ($scope.needGetStatus) {
                $scope.statusConsole();
            }
        }, 1000);
    });

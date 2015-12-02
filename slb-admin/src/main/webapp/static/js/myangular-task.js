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
        $scope.autoSwitchLogView = true;
        $scope.currentLogView = {};
        $scope.currentLogView.type = 'vs';
        $scope.currentLogView.vsName = null;
        $scope.currentLogView.agentId = null;

        $scope.getTask = function (taskId) {
            // 获取task
            $http(
                {
                    method: 'GET',
                    url: window.contextpath
                    + '/console/deploy/task/' + taskId
                    + '/get'
                })
                .success(
                function (data, status, headers, config) {
                    if (data.errorCode == 0) {
                        $scope.task = data.task;
                        // 展现出来
                        $(
                            '#TaskController > div.main-content')
                            .show();

                        if ($scope.task.task.status == 'CREATED') {
                            $scope.canUpdate = true;
                        }
                        // 开始ajax论询获取task的状态
                        if (!$scope.canUpdate) {
                            $scope.needGetStatus = true;
                        }
                    } else {
                        app.alertError("获取失败: "
                            + data.errorMessage);
                    }
                })
                .error(function (data, status, headers, config) {
                    app.appError("响应错误", data);
                });
        };
        $scope.isContain = function (deployAgentBos, ip) {
            return deployAgentBos[ip] != null;
        }
        $scope.checkIp = function (deployAgentBos, ip) {
            if (!$scope.canUpdate) {
                return;
            }
            if (deployAgentBos[ip] != null) {
                delete deployAgentBos[ip];
            } else {
                deployAgentBos[ip] = {
                    "deployAgent": {
                        "ipAddress": ip
                    }
                };
            }
        }
        $scope.isContainAll = function (deployAgentBos, instances) {
            var isContainAll = true;
            $.each(instances, function (j, instance) {
                var contain = deployAgentBos[instance.ip] != null;
                if (!contain) {
                    isContainAll = false;
                    return false;
                }
            });
            return isContainAll;
        }
        $scope.checkAllIp = function (deployVsBo) {
            deployVsBo.deployAgentBos = {};
            var instances = deployVsBo.slbPool.instances;
            $.each(instances, function (i, instance) {
                deployVsBo.deployAgentBos[instance.ip] = {
                    "deployAgent": {
                        "ipAddress": instance.ip
                    }
                };
            });
            deployVsBo.checkAllIp = true;
        }
        $scope.uncheckAllIp = function (deployVsBo) {
            deployVsBo.deployAgentBos = {};
            deployVsBo.checkAllIp = false;
        }
        $scope.batchCheckAllIp = function () {
            $
                .each(
                $scope.task.deployVsBos,
                function (i, deployVsBo) {
                    deployVsBo.deployAgentBos = {};
                    var instances = deployVsBo.slbPool.instances;
                    $
                        .each(
                        instances,
                        function (i,
                                  instance) {
                            deployVsBo.deployAgentBos[instance.ip] = {
                                "deployAgent": {
                                    "ipAddress": instance.ip
                                }
                            };
                        });
                });
            $scope.allChecked = true;
        }
        $scope.batchUncheckAllIp = function () {
            $.each($scope.task.deployVsBos,
                function (i, deployVsBo) {
                    deployVsBo.deployAgentBos = {};
                });
            $scope.allChecked = false;
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

        $scope.getAgent = function (deployAgentBos, ip) {
            return deployAgentBos[ip];
        }
        $scope.getStatus = function (deployAgentBos, ip) {
            var deployAgentBo = deployAgentBos[ip];
            if (deployAgentBo != null) {
                if (deployAgentBo.deployAgent.status != null) {
                    return deployAgentBo.deployAgent.status;
                } else {
                    return "CREATED";
                }
            }
            return null;
        }
        // 更新Task
        $scope.updateDeployTask = function () {
            app.clearAlertMessage();
            if (!$scope.canUpdate) {// 如果不可修改，则直接返回
                console.log('Can not update task.');
                return;
            }
            $http(
                {
                    method: 'POST',
                    data: $scope.task,
                    url: window.contextpath
                    + '/console/deploy/task/'
                    + $scope.task.task.id + '/update'
                }).success(
                function (data, status, headers, config) {
                    if (data.errorCode == 0) {
                        // 设置为不可修改
                        $scope.canUpdate = false;
                        // 开始启动
                        $scope.startTask();
                    } else {
                        app.alertError("保存失败: "
                            + data.errorMessage);
                    }
                }).error(
                function (data, status, headers, config) {
                    app.appError("响应错误", data);
                });
        };
        $scope.updateAndStartTask = function () {
            if (!$scope.canUpdate) {// 如果不可修改，则直接启动
                $scope.startTask();
            } else {
                $scope.updateDeployTask();
            }
        }
        $scope.startTask = function () {
            $scope.needGetStatus = true;
            $scope.autoSwitchLogView = true;
            // $scope.batchCollapse();
            $http(
                {
                    method: 'GET',
                    url: window.contextpath
                    + '/console/deploy/task/'
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
                    + '/console/deploy/task/'
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
                    + '/console/deploy/task/'
                    + $scope.task.task.id + '/status'
                })
                .success(
                function (data, status, headers, config) {
                    if (data.errorCode == 0) {
                        $scope.task = data.task;
                        // 如果有agent处于'PROCESSING'状态，则console显示它。
                        $
                            .each(
                            $scope.task.deployVsBos,
                            function (vsName,
                                      deployVsBo) {
                                var breakFor = true;
                                $
                                    .each(
                                    deployVsBo.deployAgentBos,
                                    function (ip,
                                              deployAgentBo) {
                                        if (deployAgentBo.deployAgent.status == 'PROCESSING') {
                                            // 找到哪个vs正在运行
                                            // 切换日志
                                            if ($scope.autoSwitchLogView) {
                                                console
                                                    .log('switch log auto');
                                                // $scope.currentAgentOrVsOfLogView
                                                // =
                                                // deployVsBo.deployVs;
                                                $scope.currentLogView.type = 'vs';
                                                $scope.currentLogView.vsName = vsName;
                                            }
                                            // 显示运行的图标
                                            deployVsBo.isRunning = true;
                                            breakFor = false;
                                            return breakFor;
                                        }
                                    });
                                return breakFor;
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
        $scope.showVsLog = function (deployVsBo) {
            if (deployVsBo) {
                $scope.autoSwitchLogView = false;
                $scope.currentLogView.type = 'vs';
                $scope.currentLogView.vsName = deployVsBo.deployVs.vsName;
                $scope.currentLogView.agentId = null;
                // $scope.currentAgentOrVsOfLogView =
                // deployVsBo.deployVs;
                $scope.showLog();
            }
        }
        $scope.showAgentLog = function (deployAgentBo) {
            if (deployAgentBo) {
                $scope.autoSwitchLogView = false;
                $scope.currentLogView.type = 'agent';
                $scope.currentLogView.vsName = null;
                $scope.currentLogView.agentId = deployAgentBo.deployAgent.id;
                // $scope.currentAgentOrVsOfLogView =
                // deployAgentBo.deployAgent;
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
            $
                .each(
                $scope.task.deployVsBos,
                function (vsName, deployVsBo) {
                    var breakFor = true;
                    if ($scope.currentLogView.type == 'vs') {
                        if ($scope.currentLogView.vsName == vsName) {
                            $('#console')
                                .text(
                                deployVsBo.deployVs.summaryLog != null ? deployVsBo.deployVs.summaryLog
                                    : "");
                            breakFor = false;
                        }
                    } else {
                        $
                            .each(
                            deployVsBo.deployAgentBos,
                            function (ip,
                                      deployAgentBo) {
                                if ($scope.currentLogView.agentId == deployAgentBo.deployAgent.id) {
                                    $(
                                        '#console')
                                        .text(
                                        $scope
                                            .unescape(deployAgentBo.deployAgent.rawLog != null ? deployAgentBo.deployAgent.rawLog
                                                : ""));
                                    breakFor = false;
                                }
                                return breakFor;
                            });
                    }

                    return breakFor;
                });
        }
        $scope.rollback = function () {
            // 遍历一次，将状态为WARNING和SUCCESS的vsName和oldVsTag，加到newTask.selectedVsAndTags
            var newTask = {};
            newTask.taskName = "回滚：" + $scope.task.task.name;
            newTask.selectedVsAndTags = [];
            $.each($scope.task.deployVsBos, function (vsName,
                                                      deployVsBo) {
                if (deployVsBo.deployVs.status == 'SUCCESS' || deployVsBo.deployVs.status == 'WARNING') {
                    var oldVsTag = deployVsBo.deployVs.oldVsTag;
                    newTask.selectedVsAndTags.push({
                        "vsName": vsName,
                        "tag": oldVsTag
                    });
                }
            });

            $http(
                {
                    method: 'POST',
                    data: newTask,
                    url: window.contextpath
                    + '/console/deploy/task/add'
                })
                .success(
                function (data, status, headers, config) {
                    if (data.errorCode == 0) {
                        window.location = window.contextpath
                            + '/console/deploy/task/'
                            + data.taskId;
                    } else {
                        app.appError("创建回滚任务失败: "
                            + data.errorMessage);
                    }
                })
                .error(function (data, status, headers, config) {
                    // app.appError("响应错误", data);
                });
        }
        setInterval(function () {
            if ($scope.needGetStatus) {
                $scope.statusConsole();
            }
        }, 600);
    });

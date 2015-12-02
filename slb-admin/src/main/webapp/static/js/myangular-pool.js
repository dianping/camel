module
    .controller(
    'PoolController',
    function ($scope, DataService, $resource, $http) {
        $scope.strategies = DataService.getStrategies();
        var poolChanged = false;
        $scope.pool = null;
        $scope.getPool = function (poolName) {
            var hash = window.location.hash;
            var url = window.contextpath + '/console/pool/' + poolName
                + '/get';
            if (hash == '#showInfluencing') {
                url += '?showInfluencing=true';
            }
            $http({
                method: 'GET',
                url: url
            })
                .success(
                function (data, status, headers, config) {
                    if (data.errorCode == 0) {
                        if (data.pool == null) {// 新建pool
                            $scope.pool = new Object();
                            $scope.pool.name = poolName;
                            $scope.pool.minAvailableMemberPercentage = 50;
                            $scope.pool.loadbalanceStrategyName = "round-robin";
                            $scope.pool.check = new Object();
                            $scope.pool.check.checkHttpSend = "/inspect/healthcheck";
                            $scope.pool.check.checkHttpExpectAlive = "http_2xx";
                            $scope.pool.check.timeout = 3000;
                            $scope.pool.check.interval = 3000;
                            $scope.pool.keepalive = 20;
                            $scope.pool.check.type = "TCP";
                            $scope.newPool = true;
                        } else {
                            $scope.pool = data.pool;
                            $scope.newPool = false;
                        }
                        // 如果需要显示受影响的vs，则显示
                        $scope.influencingVsList = data.influencingVsList;
                        // 展现出来
                        $(
                            '#PoolController > div.main-content')
                            .css("opacity", 1);
                        // 开始监听pool的修改
                        $scope.$watch('pool', function (newValue, oldValue) {
                            if (newValue != oldValue) {
                                poolChanged = true;
                            }
                        }, true);
                    } else {
                        app.alertError("获取失败: "
                            + data.errorMessage);
                    }
                })
                .error(function (data, status, headers, config) {
                    app.appError("响应错误", data);
                });
        };
        // 保存
        $scope.save = function () {

            if ($scope.pool.degradeRate) {
                if ($scope.pool.degradeRate < 0 || $scope.pool.degradeRate > 100) {
                    app.alertError("pool降级比例范围0~100");
                    return;
                }
            }

            if (isNaN(parseInt($scope.pool.keepalive)) || $scope.pool.keepalive < 0) {
                app.alertError("pool长连接连接数必须为数字，且>=0");
                return;
            }


            //校验数据
            if ($scope.pool.check) {
                if ($scope.pool.check.type) {
                    if (!$scope.pool.check.timeout) {
                        app.alertError("请填写健康检查的超时时间");
                        return;
                    }
                    if (!$scope.pool.check.interval) {
                        app.alertError("请填写健康检查的时间间隔");
                        return;
                    }
                }
                if ($scope.pool.check.type == 'HTTP') {
                    if (!$scope.pool.check.checkHttpSend) {
                        app.alertError("请填写健康检查http报文");
                        return;
                    }
                    if (!$scope.pool.check.checkHttpExpectAlive) {
                        app.alertError("http返回码");
                        return;
                    }
                }
            }


            for (i in $scope.pool.members) {
                var member = $scope.pool.members[i];
                if (!member.ip) {
                    app.alertError("ip地址为空");
                    return;
                }
                if (!isIP(member.ip)) {
                    app.alertError("请输入数字类型的ip地址，例如：127.0.0.1");
                    return;
                }
            }

            $http(
                {
                    method: 'POST',
                    data: $scope.pool,
                    url: window.contextpath + '/console/pool/'
                    + $scope.pool.name + '/save'
                })
                .success(
                function (data, status, headers, config) {
                    if (data.errorCode == 0) {
                        app
                            .alertSuccess("保存成功！ 即将刷新页面...");
                        poolChanged = false;// 保存成功，修改标识重置
                        setTimeout(
                            function () {
                                window.location = window.contextpath
                                    + "/console/pool/"
                                    + $scope.pool.name
                                    + "#showInfluencing";
                            }, 200);
                    } else {
                        app.alertError("保存失败: "
                            + data.errorMessage);
                    }
                })
                .error(function (data, status, headers, config) {
                    app.appError("响应错误", data);
                });
        };
        // 删除
        $scope.removePool = function () {
            $http(
                {
                    method: 'POST',
                    data: $scope.pool,
                    url: window.contextpath + '/console/pool/'
                    + $scope.pool.name + '/remove'
                })
                .success(
                function (data, status, headers, config) {
                    if (data.errorCode == 0) {
                        app.alertSuccess(
                            "删除成功！ 即将刷新页面...");
                        setTimeout(
                            function () {
                                window.location = window.contextpath
                                    + "/console/pool";
                            }, 700);
                    } else {
                        app.alertError("删除失败: "
                            + data.errorMessage);
                    }
                })
                .error(function (data, status, headers, config) {
                    app.appError("响应错误", data);
                });
        };
        // 用於select
        $scope.getInitSelectValue = function (curValue, valueList,
                                              propertyName) {
            var re = curValue;
            if (propertyName) {
                if ((curValue == null || curValue == '')
                    && valueList && valueList.length > 0)
                    re = valueList[0][propertyName];
            } else {
                if ((curValue == null || curValue == '')
                    && valueList && valueList.length > 0) {
                    re = valueList[0];
                }
            }
            return re;
        }
        $scope.edit = function () {
            window.location = window.contextpath + '/console/pool/'
                + $scope.pool.name + '/edit';
        }
        $scope.cancleEdit = function () {
            window.location = window.contextpath + '/console/pool/'
                + $scope.pool.name;
        }
        // member增删
        $scope.addMember = function () {
            var member = new Object();
            member.state = 'ENABLED';
            member.availability = 'AVAILABLE';
            member.port = 80;
            member.weight = 1;
            member.maxFails = 3;
            member.failTimeout = '2s';
            var members = $scope.pool.members;
            if (!members) {
                members = [];
                $scope.pool.members = members;
            }
            members.push(member);
        }
        $scope.affirmRemoveMemberModal = function (index) {
            $scope.memberToBeRemove = $scope.pool.members[index];
            $scope.memberIndexToBeRemove = index;
            $('#affirmRemoveMemberModal').modal('show');
        }
        $scope.removeMember = function () {
            $scope.pool.members.splice(
                $scope.memberIndexToBeRemove, 1);
            $('#affirmRemoveMemberModal').modal('hide');
        }
        // $scope.openUrl = null;
        $scope.addTagAndDeploy = function (influencingVsList) {
            var param = new Object();
            param.vsListToTag = influencingVsList;
            var url = window.contextpath + '/console/vs/tag/addBatch?'
                + $.param(param, true);
            window.open(url);
        }

        // search
        $scope.pools = DataService.getPools(function () {
            var poolNameList = [];
            $.each($scope.pools, function (i, pool) {
                poolNameList.push(pool.name);
            });
            $("#pool-search-nav").typeahead(
                {
                    source: poolNameList,
                    updater: function (c) {
                        window.location = window.contextpath
                            + "/console/pool/" + c;
                        return c;
                    }
                })
        });
        // 在下方详细显示受影响的站点
        $scope.influencingVsList = function (poolName) {
            $http(
                {
                    method: 'GET',
                    url: window.contextpath + '/console/pool/'
                    + poolName + '/influencingVsList'
                })
                .success(
                function (data, status, headers, config) {
                    if (data.errorCode == 0) {
                        $scope.influencingVsDetailList = data.influencingVsList;
                    }
                });
        };


        // 离开页面时，对比一下pool是否发生了修改
        var onunload = function () {
            if (poolChanged) {
                return "您的修改尚未保存，现在离开将丢失所有修改";
            }
        }
        window.onbeforeunload = onunload;

    });
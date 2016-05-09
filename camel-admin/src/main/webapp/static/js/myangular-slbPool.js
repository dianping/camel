module.controller('SlbPoolController', function ($scope, DataService, $resource,
                                                 $http) {
    $scope.strategies = DataService.getStrategies();
    var slbPoolChanged = false;
    $scope.slbPool = null;
    $scope.getSlbPool = function (slbPoolName) {
        var hash = window.location.hash;
        var url = window.contextpath + '/console/slbPool/' + slbPoolName + '/get';
        if (hash == '#showInfluencing') {
            url += '?showInfluencing=true';
        }
        $http({
            method: 'GET',
            url: url
        }).success(function (data, status, headers, config) {
            if (data.errorCode == 0) {
                if (data.slbPool == null) {
                    // 新建slbPool
                    $scope.slbPool = new Object();
                    $scope.slbPool.name = slbPoolName;
                    var instance = new Object();
                    var instances = $scope.slbPool.instances;
                    if (!instances) {
                        instances = [];
                        $scope.slbPool.instances = instances;
                    }
                    instances.push(instance);

                    $scope.newSlbPool = true;
                } else {
                    $scope.slbPool = data.slbPool;
                    $scope.newSlbPool = false;
                }
                // 如果需要显示受影响的vs，则显示
                $scope.influencingVsList = data.influencingVsList;
                // 展现出来
                $('#SlbPoolController > div.main-content').show();
                // 开始监听slbPool的修改
                $scope.$watch('slbPool', function (newValue, oldValue) {
                    if (newValue != oldValue) {
                        slbPoolChanged = true;
                    }
                }, true);
            } else {
                app.alertError("获取失败: " + data.errorMessage);
            }
        }).error(function (data, status, headers, config) {
            app.appError("响应错误", data);
        });
    };
    // 保存
    $scope.save = function () {
        $http(
            {
                method: 'POST',
                data: $scope.slbPool,
                url: window.contextpath + '/console/slbPool/'
                + $scope.slbPool.name + '/save'
            }).success(
            function (data, status, headers, config) {
                if (data.errorCode == 0) {
                    app.alertSuccess("保存成功！ 即将刷新页面...");
                    slbPoolChanged = false;// 保存成功，修改标识重置
                    setTimeout(function () {
                        window.location = window.contextpath + "/console/slbPool/"
                            + $scope.slbPool.name + "#showInfluencing";
                    }, 700);
                } else {
                    app.alertError("保存失败: " + data.errorMessage);
                }
            }).error(function (data, status, headers, config) {
                app.appError("响应错误", data);
            });
    };
    // 删除
    $scope.removeSlbPool = function () {
        $http(
            {
                method: 'POST',
                data: $scope.slbPool,
                url: window.contextpath + '/console/slbPool/'
                + $scope.slbPool.name + '/remove'
            }).success(
            function (data, status, headers, config) {
                if (data.errorCode == 0) {
                    app.alertSuccess("删除成功！ 即将刷新页面...",
                        "removeSlbPoolAlertDiv");
                    setTimeout(function () {
                        window.location = window.contextpath + "/";
                    }, 700);
                } else {
                    app.alertError("删除失败: " + data.errorMessage,
                        "removeSlbPoolAlertDiv");
                }
            }).error(function (data, status, headers, config) {
                app.appError("响应错误", data);
            });
    };
    // 用於select
    $scope.getInitSelectValue = function (curValue, valueList, propertyName) {
        var re = curValue;
        if (propertyName) {
            if ((curValue == null || curValue == '') && valueList
                && valueList.length > 0)
                re = valueList[0][propertyName];
        } else {
            if ((curValue == null || curValue == '') && valueList
                && valueList.length > 0) {
                re = valueList[0];
            }
        }
        return re;
    }
    $scope.edit = function () {
        window.location = window.contextpath + '/console/slbPool/'
            + $scope.slbPool.name + '/edit';
    }
    $scope.cancleEdit = function () {
        window.location = window.contextpath + '/console/slbPool/'
            + $scope.slbPool.name;
    }
    // 存活的instance
    $scope.getAliveInstanceCount = function () {
        return '待咨询jinhua';
    }
    // instance增删
    $scope.addInstance = function () {
        var instance = new Object();
        var instances = $scope.slbPool.instances;
        if (!instances) {
            instances = [];
            $scope.slbPool.instances = instances;
        }
        instances.push(instance);
    }
    $scope.removeInstance = function (index) {
        $scope.slbPool.instances.splice(index, 1);
    }
    // $scope.openUrl = null;
    $scope.addTagAndDeploy = function (influencingVsList) {
        var param = new Object();
        param.vsListToTag = influencingVsList;
        var url = window.contextpath + '/console/vs/tag/addBatch?'
            + $.param(param, true);
        window.open(url);
        // if (!$scope.openUrl) {
        // $http({
        // method : 'POST',
        // data : $.param(param, true),
        // headers : {
        // 'Content-Type' : 'application/x-www-form-urlencoded'
        // },
        // url : window.contextpath + '/vs/tag/addBatch'
        // }).success(
        // function(data, status, headers, config) {
        // if (data.errorCode == 0) {
        // // $.each(data.tagIds, function(vsName,tagId) {
        // // openUrl += vsName
        // // });
        // // var json = JSON.stringify(data.tagIds);
        // // console.log(json);
        // $scope.openUrl = window.contextpath
        // + '/deploy#showInfluencing:'
        // + influencingVsList.join();
        // window.open($scope.openUrl);
        // } else {
        // app.alertError("创建失败: " + data.errorMessage);
        // }
        // }).error(function(data, status, headers, config) {
        // app.appError("响应错误", data);
        // });
        // } else {
        // window.open($scope.openUrl);
        // }

    }
    // 离开页面时，对比一下slbPool是否发生了修改
    var onunload = function () {
        if (slbPoolChanged) {
            return "您的修改尚未保存，现在离开将丢失所有修改";
        }
    }
    window.onbeforeunload = onunload;

});
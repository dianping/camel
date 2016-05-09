module.controller("ConfigVSController", function ($scope, DataService,
                                                  $resource, $http) {
    $scope.isVSListReady = false;
    $scope.agentPools = [];
    $scope.op_result = "";

    $scope.listAgentPools = function () {
        $scope.isVSListReady = false;
        $http({
            method: 'GET',
            url: window.contextpath + '/config/vs/clean/list'
        }).success(function (data, status, headers, config) {
            if (data.errorCode == 0) {
                $scope.agentPools = data.agentPools;
                $scope.isVSListReady = true;
                $scope.opSuccess();
            } else {
                alert("请刷新：获取集群列表失败: " + data.errorMessage);
            }
        }).error(function (data, status, headers, config) {
            alert("请刷新：响应错误", data);
        });
    }

    $scope.removeVS = function (host, vs) {
        $http(
            {
                method: 'DELETE',
                url: window.contextpath + '/config/vs/clean/delete/'
                + encodeURI(host) + '/' + encodeURI(vs)
            }).success(function (data, status, headers, config) {
                if (data.errorCode == 0) {
                    $scope.listAgentPools();
                    $scope.opSuccess();
                } else {
                    alert("请重试：删除失败: " + data.errorMessage);
                }
            }).error(function (data, status, headers, config) {
                alert("请重试：响应错误", data);
            });
    }

    $scope.removeVSByPool = function (vsPool, vs) {
        $http(
            {
                method: 'DELETE',
                url: window.contextpath + '/config/vs/clean/deleteByPool/'
                + encodeURI(vsPool) + '/' + encodeURI(vs)
            }).success(function (data, status, headers, config) {
                if (data.errorCode == 0) {
                    $scope.listAgentPools();
                    $scope.opSuccess();
                } else {
                    alert("请重试：删除失败: " + data.errorMessage);
                }
            }).error(function (data, status, headers, config) {
                alert("请重试：响应错误", data);
            });
    }

    $scope.opSuccess = function () {
        $scope.op_result = "success";
        setTimeout(function () {
            $scope.op_result = "";
            $scope.$digest();
        }, 3000);
    }

    $scope.checkAll = function (agent) {
        $.each(agent.vsList, function (i, vs) {
            vs.selected = true;
        })
        agent.checkAll = true;
    }

    $scope.uncheckAll = function (agent) {
        $.each(agent.vsList, function (i, vs) {
            vs.selected = false;
        })
        agent.checkAll = false;
    }


    $scope.checkVS = function (vs) {
        if (vs.selected) {
            vs.selected = false;
        } else {
            vs.selected = true;
        }
    }

    $scope.removeByAgent = function (agentPoolName, agent) {
        var agentName = agent.host;
        var isFirst = true;
        var selectedVS = "";

        $.each(agent.vsList, function (i, vs) {
            if (vs.selected) {
                if (isFirst) {
                    selectedVS = vs.name;
                    isFirst = false;
                } else {
                    selectedVS += "," + vs.name;
                }
            }
        })

        var url;

        if (agentName == "COMMON") {
            url = window.contextpath + '/config/vs/clean/batchDeleteByPool/'
                + encodeURI(agentPoolName);
        } else {
            url = window.contextpath + '/config/vs/clean/batchDelete/'
                + encodeURI(agentName);
        }
        $http({
            method: 'DELETE',
            url: url,
            data: selectedVS
        }).success(function (data, status, headers, config) {
            if (data.errorCode == 0) {
                $scope.listAgentPools();
                $scope.opSuccess();
            } else {
                alert("请重试：删除失败: " + data.errorMessage);
            }
        }).error(function (data, status, headers, config) {
            alert("请重试：响应错误", data);
        });
    }

    $scope.listAgentPools();
});
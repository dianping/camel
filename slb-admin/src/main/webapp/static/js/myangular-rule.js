module.controller("RuleController", function ($scope, DataService, $resource,
                                              $http) {
    $scope.statusCodes = [];
    $scope.selectedPools = [];
    $scope.filterPools = [];
    $scope.currentRule = {};

    $scope.listStatusCodes = function () {
        $http({
            method: 'GET',
            url: window.contextpath + '/monitor/statuscode/get'
        }).success(function (data, status, headers, config) {
            if (data.errorCode == 0) {
                $scope.statusCodes = data.statusCodes;
            } else {
                alert("获取集群列表失败: " + data.errorMessage);
            }
        }).error(function (data, status, headers, config) {
            alert("响应错误", data);
        });
    }
    $scope.getRule = function () {
        $http({
            method: 'GET',
            url: window.contextpath + '/monitor/status/rule/get'
        }).success(function (data, status, headers, config) {
            if (data.errorCode == 0) {
                if (data.rules != null) {
                    $scope.rules = data.rules;
                }
                $('#RuleController > div.main-content').show();
            } else {
                app.appError("响应错误", data);
            }
        }).error(function (data, status, headers, config) {
            app.appError("响应错误", data);
        });
    };
    $scope.add = function () {
        $scope.editOrShow = "edit";
        $scope.currentRule = {};
    }
    $scope.cancleEdit = function () {
        window.location = window.contextpath + '/monitor/status/rule';
    }
    $scope.edit = function (index) {
        $scope.editOrShow = "edit";
        $scope.currentRule = $scope.rules[index];
        if ($scope.currentRule.hasOwnProperty("pool")) {
            var rawPoolStr = $scope.currentRule.pool;
            var rawPoolList = rawPoolStr.split(",");

            $.each(rawPoolList, function (n, v) {
                if (v != null && v != "") {
                    $scope.selectedPools.push(v);
                }
            })
        }
        if ($scope.currentRule.hasOwnProperty("filterPool")) {
            var rawPoolStr = $scope.currentRule.filterPool;
            var rawPoolList = rawPoolStr.split(",");

            $.each(rawPoolList, function (n, v) {
                if (v != null && v != "") {
                    $scope.filterPools.push(v);
                }
            })
        }
    }
    $scope.remove = function (index) {
        $http({
            method: 'DELETE',
            url: window.contextpath + '/monitor/status/rule/remove/' + $scope.rules[index].id
        }).success(function (data, status, headers, config) {
            if (data.errorCode == 0) {
                window.location = window.contextpath + '/monitor/status/rule';
            } else {
                app.appError("删除失败: " + data.errorMessage);
            }
        }).error(function (data, status, headers, config) {
            app.appError("响应错误", data);
        });
    }
    $scope.removeSelectedPool = function (index) {
        $scope.selectedPools.splice(index, 1);
    }
    $scope.removeFilterPool = function (index) {
        $scope.filterPools.splice(index, 1);
    }
    $scope.save = function () {
        var poolRawStr = "";
        $.each($scope.selectedPools, function (n, value) {
            if (value != null) {
                poolRawStr += value + ",";
            }
        });
        $scope.currentRule.pool = poolRawStr;

        var filterPoolRawStr = "";
        $.each($scope.filterPools, function (n, value) {
            if (value != null) {
                filterPoolRawStr += value + ",";
            }
        });
        $scope.currentRule.filterPool = filterPoolRawStr;
        $http({
            method: 'POST',
            data: $scope.currentRule,
            url: window.contextpath + '/monitor/status/rule/save'
        }).success(function (data, status, headers, config) {
            if (data.errorCode == 0) {
                window.location = window.contextpath + '/monitor/status/rule';
            } else {
                app.appError("保存失败: " + data.errorMessage);
            }
        }).error(function (data, status, headers, config) {
            app.appError("响应错误", data);
        });
    };
    // search
    $scope.pools = DataService.getPools(function () {
        var poolNameList = ['All'];
        $.each($scope.pools, function (i, pool) {
            poolNameList.push(pool.name);
        });
        $("#pool-search-nav").typeahead({
            source: poolNameList,
            updater: function (c) {
                if (c == 'All') {
                    $scope.selectedPools = ['All'];
                    $scope.$apply();
                } else {
                    var containsC = false;
                    var containsAll = false;

                    $.each($scope.selectedPools, function (n, v) {
                        if (v == 'All') {
                            containsAll = true;
                        }
                        if (v == c) {
                            containsC = true;
                        }
                    });
                    if (c != null && !containsC && !containsAll) {
                        $scope.selectedPools.push(c);
                        $scope.$apply();
                    }
                    ;
                }
            }
        });
        $("#pool-search-filter-nav").typeahead({
            source: poolNameList,
            updater: function (c) {
                var containsC = false;

                $.each($scope.filterPools, function (n, v) {
                    if (v == c) {
                        containsC = true;
                    }
                });
                if (c != null && !containsC) {
                    $scope.filterPools.push(c);
                    $scope.$apply()
                }
                ;
            }
        });
    });
    $scope.showFilterPools = function () {
        var isShow = false;
        $.each($scope.selectedPools, function (n, v) {
            if (v == 'All') {
                isShow = true;
            }
        });
        return isShow;
    }
    $scope.listStatusCodes();
    $scope.getRule();
});
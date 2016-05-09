module.controller('ProfileController', function ($scope, DataService, $resource,
                                                 $http) {
    // 计算状态
    $scope.getVsState = function () {
        return '待询问jinhua';
    }
    // 动态参数的管理
    $scope.propertiesDefinedInputs = DataService.getPropertiesDefinedInputs();
    $scope.addDynamicAttribute = function (key, value) {
        if (key == null || key.trim() == '') {
            app.alertError("参数名不能为空！", "addParamAlertDiv");
            return;
        }
        key = key.trim();
        if ($scope.vs.dynamicAttributes[key] != null) {
            app.appError('通知', "该参数名( " + key + " )已经存在，不能添加！");
        } else {
            if (value != null) {
                $scope.vs.dynamicAttributes[key] = value;
            } else {
                $scope.vs.dynamicAttributes[key] = '';
            }
        }
        $('#addParamModal').modal('hide');
    }
    $scope.addNewDynamicAttribute = function () {
        $scope.addDynamicAttribute($('#addParamKey').val(), $('#addParamValue')
            .val());
    }
    $scope.removeDynamicAttribute = function (key) {
        delete $scope.vs.dynamicAttributes[key];
    }
    $scope.getInputType = function (key) {
        // console.log($scope.definedParamMap);
        var propertiesDefinedInputs = $scope.propertiesDefinedInputs[key];
        if (propertiesDefinedInputs == null) {
            return 'TEXT';
        }
        var inputType = propertiesDefinedInputs.inputType;
        return inputType;
    }
    $scope.getValueList = function (name) {
        var propertiesDefinedInputs = $scope.propertiesDefinedInputs[name];
        if (propertiesDefinedInputs) {
            return valueList = propertiesDefinedInputs.valueList;
        }
        return [];
    }
    // instance
    $scope.removeInstance = function (index) {
        $scope.vs.instances.splice(index, 1);
    }
    $scope.addInstance = function () {
        var instance = new Object();
        instance.ip = '';
        $scope.vs.instances.push(instance);
    }
    // 默认pool选择
    $scope.pools = DataService.getPools(function () {
        // if ($scope.pools.length > 0) {
        // $scope.vs.defaultPoolName = $scope.pools[0];
        // }
    });
    // 默认pool选择
    $scope.slbPools = DataService.getSlbPools(function () {
        // if ($scope.slbPools.length > 0) {
        // $scope.vs.slbPool = $scope.slbPools[0];
        // }
    });
});

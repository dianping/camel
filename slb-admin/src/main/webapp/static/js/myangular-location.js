module
    .controller(
    'LocationController',
    function ($scope, DataService, $resource, $http) {
        // location视图切换
        $scope.locationEditing = null;
        $scope.switchLocation = function (index) {
            $scope.locationEditing = $scope.vs.locations[index];
            $('html, body').animate({
                scrollTop: 0
            }, 100);
        };
        $scope.switchLocationList = function () {
            $scope.locationEditing = null;
        }
        // location增删
        $scope.openAddLocationModal = function () {
            $scope.locationToBeAdd = new Object();
            $scope.locationToBeAdd.caseSensitive = true;
            $scope.locationToBeAdd.matchType = 'prefix';
            $('#addLocationModal').modal('show');
            $('#addLocationPatternInput').focus();
        };
        $scope.addLocation = function () {
            var matchType = $scope.locationToBeAdd.matchType;
            if (matchType == null || matchType.trim() == '') {
                app.alertError("匹配类型必选！", "addLocationAlertDiv");
                return;
            }
            var pattern = $scope.locationToBeAdd.pattern;
            if (pattern == null || pattern.trim() == '') {
                app.alertError("正则表达式必填！", "addLocationAlertDiv");
                return;
            }
            $scope.vs.locations.push($scope.locationToBeAdd);
            $('#addLocationModal').modal('hide');
        }
        $scope.affirmRemoveLocationModal = function (index) {
            $scope.locationToBeRemove = $scope.vs.locations[index];
            $scope.locationIndexToBeRemove = index;
            $('#affirmRemoveLocationModal').modal('show');
        }
        $scope.removeLocation = function () {
            $scope.vs.locations.splice(
                $scope.locationIndexToBeRemove, 1);
            $('#affirmRemoveLocationModal').modal('hide');
        }
        // directive增删
        $scope.directiveDefinedInputs = DataService
            .getDirectiveDefinedInputs();
        $scope.getInputs = function (type) {
            return $scope.directiveDefinedInputs[type];
        }
        $scope.getValueList = function (input) {
            if (!input) {
                return;
            }
            if (input.name == 'pool-name') {// 对pool-name特殊处理
                var list = [];
                for (var i = 0; i < $scope.pools.length; i++) {
                    list.push($scope.pools[i].name);
                }
                //唤醒chosen-select
                setTimeout(function () {
                    $(".chosen-select").chosen()
                }, 1500);//由于angularjs加载option也是延迟的，所以这个操作得更迟才行。
                return list;
            }
            return input.valueList;
        }
        $scope.openAddDirectiveModal = function () {
            $scope.directiveToBeAdd = new Object();
            $scope.directiveToBeAdd.dynamicAttributes = {};
            for (first in $scope.directiveDefinedInputs)
                break;
            $scope.directiveToBeAdd.type = first;
            $('#addDirectiveModal').modal('show');
            $('#addDirectiveType').focus();
        };
        $scope.addDirective = function () {
            var directives = $scope.locationEditing.directives;
            if (!directives) {
                directives = [];
                $scope.locationEditing.directives = directives;
            }
            // 根据勾选的type，找到inputs模板
            var inputs = $scope.directiveDefinedInputs[$scope.directiveToBeAdd.type];
            // 给$scope.directiveToBeAdd赋予空的inputs模板的键值对
            for (var name in inputs) {
                $scope.directiveToBeAdd.dynamicAttributes[name] = "";
            }
            directives.push($scope.directiveToBeAdd);
            $('#addDirectiveModal').modal('hide');
        }
        $scope.affirmRemoveDirectiveModal = function (index) {
            $scope.directiveToBeRemove = $scope.locationEditing.directives[index];
            $scope.directiveIndexToBeRemove = index;
            $('#affirmRemoveDirectiveModal').modal('show');
        }
        $scope.removeDirective = function () {
            $scope.locationEditing.directives.splice(
                $scope.directiveIndexToBeRemove, 1);
            $('#affirmRemoveDirectiveModal').modal('hide');
        }
        // 指令下的属性的增删
        $scope.addDynamicAttribute = function (directive, name) {
            if (directive.dynamicAttributes[name] != null) {
                app
                    .appError('通知', "该参数名( " + name
                    + " )已经存在，不能添加！");
            } else {
                directive.dynamicAttributes[name] = '';
            }
        }
        $scope.removeDynamicAttribute = function (directive, name) {
            delete directive.dynamicAttributes[name];
        }
        // pool-name选择
        $scope.pools = DataService.getPools();

    });

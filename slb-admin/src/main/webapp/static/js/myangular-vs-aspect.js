module
    .controller(
    'VsAspectController',
    function ($scope, DataService, $resource, $http) {
        // aspect视图切换
        $scope.aspectEditing = null;
        $scope.switchAspect = function (index) {
            $scope.aspectEditing = $scope.vs.aspects[index];
        };
        $scope.switchAspectList = function () {
            $scope.aspectEditing = null;
        }
        // aspect增删
        $scope.openAddAspectModal = function () {
            $scope.aspectToBeAdd = new Object();
            $scope.aspectToBeAdd.pointCut = "BEFORE";
            $('#addAspectModal').modal('show');
            $('#addAspectName').focus();
        };
        $scope.openAddRefAspectModal = function () {
            $scope.aspectToBeAdd = new Object();
            if ($scope.aspects.length > 0) {
                $scope.aspectToBeAdd.ref = $scope.aspects[0].name;
            }
            $('#addRefAspectModal').modal('show');
        };
        $scope.addAspect = function () {
            var name = $scope.aspectToBeAdd.name;
            if (name == null || name.trim() == '') {
                app.alertError("规则名必填！", "addAspectAlertDiv");
                return;
            }
            $scope.vs.aspects.push($scope.aspectToBeAdd);
            $('#addAspectModal').modal('hide');
        }
        $scope.addRefAspect = function () {
            var ref = $scope.aspectToBeAdd.ref;
            if (ref == null || ref.trim() == '') {
                app.alertError("规则必选！", "addRefAspectAlertDiv");
                return;
            }
            $scope.vs.aspects.push($scope.aspectToBeAdd);
            $('#addRefAspectModal').modal('hide');
        }
        $scope.affirmRemoveAspectModal = function (index) {
            $scope.aspectToBeRemove = $scope.vs.aspects[index];
            $scope.aspectIndexToBeRemove = index;
            $('#affirmRemoveAspectModal').modal('show');
        }
        $scope.removeAspect = function () {
            $scope.vs.aspects.splice($scope.aspectIndexToBeRemove,
                1);
            $('#affirmRemoveAspectModal').modal('hide');
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
                    $(".chosen-select").chosen();
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
            $('#addAspectDirectiveModal').modal('show');
            $('#addAspectDirectiveType').focus();
        };
        $scope.addDirective = function () {
            var directives = $scope.aspectEditing.directives;
            if (!directives) {
                directives = [];
                $scope.aspectEditing.directives = directives;
            }
            // 根据勾选的type，找到inputs模板
            var inputs = $scope.directiveDefinedInputs[$scope.directiveToBeAdd.type];
            // 给$scope.directiveToBeAdd赋予空的inputs模板的键值对
            for (var name in inputs) {
                $scope.directiveToBeAdd.dynamicAttributes[name] = "";
            }
            directives.push($scope.directiveToBeAdd);
            $('#addAspectDirectiveModal').modal('hide');
        }
        $scope.affirmRemoveDirectiveModal = function (index) {
            $scope.directiveToBeRemove = $scope.aspectEditing.directives[index];
            $scope.directiveIndexToBeRemove = index;
            $('#affirmRemoveAspectDirectiveModal').modal('show');
        }
        $scope.removeDirective = function () {
            $scope.aspectEditing.directives.splice(
                $scope.directiveIndexToBeRemove, 1);
            $('#affirmRemoveAspectDirectiveModal').modal('hide');
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
        // 预设公共规则选择
        $scope.aspects = DataService.getAspects();
        // 查询ref的pointCut
        $scope.getPointCut = function (ref) {
            for (var i = 0; i < $scope.aspects.length; i++) {
                var aspect = $scope.aspects[i];
                if (aspect.name == ref) {
                    return aspect.pointCut;
                }
            }
        }
        $scope.displayPointCut = function (pointCut) {
            if (pointCut == 'AFTER') {
                return '后置规则';
            } else {
                return '前置规则';
            }
        }

    });

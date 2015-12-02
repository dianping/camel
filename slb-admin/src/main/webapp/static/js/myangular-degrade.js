module.controller('degradeController', function ($scope, $resource, $http) {

    $scope.ups = {};
    $scope.isCheckAll = false;

    $scope.add = function (dengine, upstreamName) {

        if ($scope.ups[dengine] == undefined) {
            $scope.ups[dengine] = {};
        }
        $scope.ups[dengine][upstreamName] = false;
    }

    $scope.check = function (dengine, upstreamName) {
        if ($scope.ups[dengine][upstreamName]) {
            $scope.ups[dengine][upstreamName] = false;
        } else {
            $scope.ups[dengine][upstreamName] = true;
        }
    }
    $scope.checkAll = function () {
        $.each($scope.ups, function (dengine, value) {
            $.each(value, function (upstreamName, value) {
                $scope.ups[dengine][upstreamName] = true;
            })
        });
        $scope.isCheckAll = true;
    }
    $scope.uncheckAll = function () {

        $.each($scope.ups, function (dengine, value) {
            $.each(value, function (upstreamName, value) {
                $scope.ups[dengine][upstreamName] = false;
            })
        });
        $scope.isCheckAll = false;
    }
});

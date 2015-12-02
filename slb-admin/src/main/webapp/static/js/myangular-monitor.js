module.controller('MonitorController', function ($scope, $http) {

    $scope.getQps = function (poolName) {
        $http({
            method: 'GET',
            url: window.contextpath + '/monitor/qps/pool/' + poolName + '/get'
        }).success(function (data, status, headers, config) {
            qps = data.qps;
            if (data.errorCode == 0) {
                $(function () {
                    $('#container').highcharts({
                        title: {
                            text: qps.title,
                            x: 0 //center
                        },
                        subtitle: {
                            text: qps.subTitle,
                            x: -20
                        },
                        xAxis: {
                            type: 'datetime'
                        },
                        yAxis: {
                            title: {
                                text: 'QPS'
                            },
                            plotLines: [{
                                value: 0,
                                width: 10,
                                color: '#808080'
                            }]
                        },
                        tooltip: {
                            valueSuffix: ''
                        },
                        legend: {
                            layout: 'vertical',
                            align: 'right',
                            verticalAlign: 'middle',
                            borderWidth: 0
                        },
                        plotOptions: {
                            series: {
                                pointStart: qps.plotOption.series.pointStart + 8 * 3600 * 1000,
                                pointInterval: qps.plotOption.series.pointInterval // one day
                            }
                        },
                        series: qps.series
                    });
                });
            } else {
                alert("获取失败: " + data.errorMessage);
//				app.alertError("获取失败: " + data.errorMessage);
            }
        }).error(function (data, status, headers, config) {

            alert("响应错误", data);
//			app.appError("响应错误", data);
        });
    }
});

module.controller('singleApp', function ($scope, $http) {

    $scope.getSinleAppStatus = function (app) {
        $http({
            method: 'GET',
            url: window.contextpath + '/monitor/singleapp/' + app + '/status/get'
        }).success(function (data, status, headers, config) {

            for (var i = 0; i < data.length; i++) {
                data[i].id = i + 1;
            }

            $scope.tengines = data;
            $scope.options = ['up', 'down', 'warning'];

        }).error(function (data, status, headers, config) {

            alert("响应错误", data);
        });
    }

});
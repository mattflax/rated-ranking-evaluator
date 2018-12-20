(function () {
    angular.module('myApp').factory('DataService', DataService);

    DataService.$inject = ['$log', '$http', '$q', 'ConfigService'];

    function DataService($log, $http, $q, ConfigService) {

        init();

        return {
            getData: getData,
            getMetricList: getMetricList,
            getVersionList: getVersionList,
            getCorpusList: getCorpusList,
            getTopicList: getTopicList,
            getQueryGroupList: getQueryGroupList,
            filterEvaluationData: filterEvaluationData
        };

        ////////////

        /**
         * Init
         */
        function init() {
            $log.log("DataService", "starting");
        }

        function getByUrl(url, config = {}) {
            var deferred = $q.defer();

            $http.get(url, config)
            .then(function(response) {
                deferred.resolve(response.data);
            },
            function(error) {
                $log.error("DataService", "Error from URL : " + url + " - " + error);
                deferred.reject(error);
            });

            return deferred.promise;
        }

        function getData() {
            return getByUrl(ConfigService.requestUrl);
        }

        function getMetricList() {
            return getByUrl(ConfigService.metricListUrl);
        }

        function getVersionList() {
            return getByUrl(ConfigService.versionListUrl);
        }

        function getCorpusList() {
            return getByUrl(ConfigService.corpusListUrl);
        }

        function getTopicList(corpus) {
            return getByUrl(ConfigService.topicListUrl, { params: { corpus: corpus }});
        }

        function getQueryGroupList(corpus, topic) {
            return getByUrl(ConfigService.queryGroupListUrl, { params: { corpus: corpus, topic: topic }});
        }

        function filterEvaluationData(corpora, topics, queryGroups, metrics, versions) {
            var deferred = $q.defer();

            var data = {
                corpora: corpora,
                topics: topics,
                queryGroups: queryGroups,
                metrics: metrics,
                versions: versions
            };

            var config = {
                headers: {
                    'Content-type': 'application/json'
                }
            };

            $http.post(ConfigService.filterUrl, data, config)
            .then(function(response) {
                deferred.resolve(response.data);
            },
            function(error) {
                $log.error("DataService", "Error filtering content - " + error);
                deferred.reject(error);
            });

            return deferred.promise;
        }
    }
})();

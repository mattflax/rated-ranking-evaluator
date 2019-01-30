(function () {
    angular.module('myApp').factory('ConfigService', ConfigService);

    ConfigService.$inject = ['$log'];

    function ConfigService($log, $http) {

        /**
         * Request interval in milliseconds
         * @type {number}
         */
        var requestInterval = 60000;

        /**
         * The data request URL
         * @type {string}
         */
        var requestUrl = "/evaluation";

        /**
         * The metric list URL.
         * @type {string}
         */
        var metricListUrl = requestUrl + "/metricList";

        /**
         * The version list URL.
         * @type {string}
         */
        var versionListUrl = requestUrl + "/versionList";

        /**
         * The corpus list URL.
         * @type {string}
         */
        var corpusListUrl = requestUrl + "/corpusList";

        /**
         * The topic list URL, requires corpus as a query param.
         * @type {string}
         */
        var topicListUrl = requestUrl + "/topicList";

        /**
         * The query group list URL, requires topic and corpus as query params.
         * @type {string}
         */
        var queryGroupListUrl = requestUrl + "/queryGroupList";

        /**
         * The filter evaluationURL.
         * Takes corpus, topic, query group, versions and metrics as query
         * params, all optional.
         * @type {string}
         */
        var filterUrl = requestUrl + "/filter";

        init();

        return {
            requestInterval: requestInterval,
            requestUrl: requestUrl,
            metricListUrl: metricListUrl,
            versionListUrl: versionListUrl,
            corpusListUrl: corpusListUrl,
            topicListUrl: topicListUrl,
            queryGroupListUrl: queryGroupListUrl,
            filterUrl: filterUrl
        };

        ////////////

        /**
         * Init
         */
        function init() {
            $log.log("ConfigService", "starting");
        }

    }
})();

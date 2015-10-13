(->

    FilesCtrl = ($scope, $http) ->

        @modalFileView = undefined
        @files = []

        @refreshFiles =
            (data, status) =>
                @files = data
                $scope.$apply()


        @getFiles = () => $.get '/getFiles', @refreshFiles

        @getFiles()

        @deleteFile =
            (f) => $.get '/deleteFile', { file: f.name }, () => @getFiles()

        @downloadFile =
            (f) =>
                ref = '/downloadFile?' + $.param({file: f.name})
                $('#download').attr({target: '_blank', href  : ref})
                $('#download')[0].click()

        @viewFile = (file) => @modalFileView = file

        ## For file uploading
        uploadSettings =
            url: 'upload'
            dataType: 'json'
            done: (e, data) => @getFiles()
            progressall: (e, data) ->
                progress = parseInt(data.loaded / data.total * 100, 10)
                $('#progress .progress-bar').css('width',progress + '%')

        @resetFileProgressBar = () -> $('#progress .progress-bar').css('width',0 + '%')

        $('#fileupload').fileupload(uploadSettings)


        return


    angular
        .module('App')
        .controller('FilesCtrl' , FilesCtrl)
)()

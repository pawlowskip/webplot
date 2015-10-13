$ ->


    ##$('.checkbox').checkbox();

    ### znikanie i pojawianie się buttona - remove dla paneli wykresów
    plotsPanel = $('#plotsPanel')
    plotsPanel.each (index, element) =>
        button = $(element).children("button[name='remove']")
        button.hide()
        $(element).hover(
          (ev) ->
            button.show()
          (ev) ->
            button.hide()
        )
    ###







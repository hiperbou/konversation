import kotlinx.html.div
import kotlinx.html.dom.append
import org.w3c.dom.Node
import kotlinx.browser.document
import kotlinx.browser.window

import ace
import setValue
import jsObject
import kotlinx.browser.document
import kotlinx.html.button

fun main() {
    val conv = """
            
            @Start

              Halt. Are you here on official business?

             

            * Hi! I'm selling fine leather jackets like the one I'm wearing. -> Jacket

            * Quickly, out of the way, I have a message for the Colonel!  -> Quickly

            * Where is the prisoner? I've come to interrogate him.        -> Prisoner

             

            @ Jacket

            Jacket? What? Who let you in here?
            

            * I just wandered in. There was no one at the door -> Goodbye
            
            * I've got authorization. How else I would get here? -> Authorization
            


            @ Authorization
             
              Well, that's true enough. How much are the jackets?
              
              
            * Only 2 Marks each! A real bargain for our brave troops! -> Goodbye
            
            * 15 Marks. Just the thing for guard duty on cold nights. -> Goodbye
            
            * For this fine leather, 300 Marks, payable in advance. -> Goodbye
             
             
             
            @ Quickly

              Sorry!
              
            -> Goodbye



            @ Prisoner

              They're holding him upstairs. But who are you?

            -> End
             
             
             
            @ Goodbye
              
              Er... you may go now sir, sorry.
              
            -> End
            
            @ End

            
        """.trimIndent()
    WebEditor(conv)
}

fun Node.sayHello() {
    append {
        div {
            +"Hello from JS"
        }
    }
}



class WebEditor(conv:String) {

    fun clearOptions() {
        document.getElementById("options")?.apply {
            while (firstChild != null) {
                removeChild(firstChild!!)
            }

        }
    }

    fun addOptionButton(parentNode:Node?, option:Option, callback:(String)->Unit) {
        parentNode!!.append {
            button { +option.name }.addEventListener("click", { _ ->
                callback(option.goto)
            })
        }
    }

    fun run(resultEditor:Editor, compiledEditor:Editor, conversationRunner:ConversationRunner) {
        clearOptions()
        val state = conversationRunner.run()
        resultEditor.setValue(resultEditor.getValue() + "--------------\n" + state.toString() + "\n--------------\n", -1)

        val options = state.sections.last().options
        if (options.isNotEmpty()) {
            document.getElementById("options")?.let {
                options.forEach { option ->
                    addOptionButton(it, option) {
                        try {
                            conversationRunner.selectOption(it)
                            run(resultEditor, compiledEditor, conversationRunner)
                        } catch (e:Exception) {
                            compiledEditor.setValue(e.message!!)
                            resultEditor.setValue("")
                        }
                    }
                }
            }
        }
    }

    init {
        document.addEventListener("DOMContentLoaded", { e ->
            val sourcesEditor = initializeEditor("source").apply {
                setValue(conv, -1)
            }
            val compiledEditor = initializeEditor("compiled", true)
            val resultEditor = initializeEditor("result", true)

            fun buildAndRun(run:Boolean) {
                try {
                    resultEditor.setValue("")
                    val conversation = parseConversation(sourcesEditor.getValue())
                    println(conversation)
                    compiledEditor.setValue(conversation.toString(), -1)

                    if (!run) return
                    val conversationRunner = ConversationRunner(conversation)
                    run(resultEditor, compiledEditor, conversationRunner)

                    //val state = run(program)
                    //resultEditor.setValue(state.format(), -1)
                } catch (e:Exception) {
                    compiledEditor.setValue(e.message!!)
                    resultEditor.setValue("")
                }
            }

            document.getElementById("compile")?.addEventListener("click", { _ ->
                buildAndRun(false)
            })

            document.getElementById("run")?.addEventListener("click", { _ ->
                buildAndRun(true)
            })
        })
    }

    private fun initializeEditor(name:String, readonly:Boolean = false) =
        ace.edit(name).apply {
            setReadOnly(readonly)
            //setTheme("ace/theme/monokai")
            setOptions(jsObject(
                "enableBasicAutocompletion" to true,
                "enableLiveAutocompletion" to true
            ))
            //session.setMode("ace/mode/java")
            session.setUseWrapMode(true)
        }
}
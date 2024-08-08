
data class ConversationState(val sections:MutableList<Section> = mutableListOf()) {

    fun Section.prettyPrint():String {
        return """${text}
            |
            |${options.joinToString("\n")}
        """.trimMargin()
    }

    override fun toString(): String {
        return sections.joinToString("\n") { it.prettyPrint() }
    }
    fun add(section:Section) = sections.add(section)
}

class ConversationRunner(private val conversation:Conversation) {
    private var finished = false
    private var currentSection:Section = conversation.sections.first()

    fun reset() {
        finished = false
        currentSection = conversation.sections.first()
    }

    fun run(state:ConversationState = ConversationState()):ConversationState {
        if (finished) return ConversationState()
        state.add(currentSection)
        currentSection.apply {
            println(text)
            if (options.isNotEmpty()) {
                options.forEach {
                    println(it)
                }
            }
            if (goto.isNotEmpty()) {
                selectOption(goto)
                run(state)
            }
        }
        return state
    }

    fun selectOption(option:String) {
        goToSection(option)
    }

    private fun goToSection(goto:String) {
        if (goto == "End") { finished = true; return }
        currentSection = conversation.sections.firstOrNull { it.name == goto } ?:
                throw Exception("Section $goto doesn't exists.\nCalled from section ${currentSection.name} with options ${currentSection.options}")
    }

    fun hasFinished():Boolean = finished
}

data class Conversation(val sections:List<Section>)
data class Option(val name:String, val goto:String) {
    override fun toString(): String {
        return "* $name -> $goto"
    }
}
data class Section(val name:String, val text:String, val options:List<Option>, val goto:String)

class ConversationBuilder {
    private val sections = mutableListOf<Section>()
    private lateinit var sectionBuilder:SectionBuilder

    fun addSection(name:String) {
        if (::sectionBuilder.isInitialized) sections.add(sectionBuilder.build())
        sectionBuilder = SectionBuilder()
        sectionBuilder.addSection(name)
    }
    fun addOption(option:String) {
        sectionBuilder.addOption(option)
    }
    fun addText(text:String) {
        sectionBuilder.addText(text)
    }
    fun addGoto(goto:String) {
        sectionBuilder.addGoto(goto)
    }

    fun build() = Conversation(sections)
}

class SectionBuilder{
    private val sectionName = mutableListOf<String>()
    private val options = mutableListOf<Option>()
    private val texts = mutableListOf<String>()
    private val gotos = mutableListOf<String>()

    fun addSection(name:String) {
        sectionName.add(name)
    }
    fun addOption(option:String) {
        val (text, goto) = option.split("->").map { it.trim() }
        options.add(Option(text, goto))
    }
    fun addText(text:String) {
        texts.add(text)
    }
    fun addGoto(goto:String) {
        gotos.add(goto)
    }

    fun build() = Section(sectionName.first(), texts.joinToString("\n"), options, gotos.firstOrNull().orEmpty())
}

fun parseConversation(text:String) = with(ConversationBuilder()) {
    text.lineSequence().forEach {
        val line = it.trim()
        when{
            line.isBlank() -> Unit
            line.startsWith("@") -> addSection(line.removePrefix("@").trim())
            line.startsWith("*") -> addOption(line.removePrefix("*").trim())
            line.startsWith("->") -> addGoto(line.removePrefix("->").trim())
            else -> addText(line)
        }
    }
    build()
}
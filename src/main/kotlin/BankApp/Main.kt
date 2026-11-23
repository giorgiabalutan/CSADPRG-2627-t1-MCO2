package BankApp

/********************
 * Last names: Avila
 * Language: Kotlin
 * Paradigm(s): Object-Oriented
 ********************/

/**
    Main Function.

    Runs the core loop of the program.
 */
fun main(){
    var input: String = ""
    var model: Model = Model()

    //Load 
    model.load()

    while (model.running){
        printScreen(model.screen)
        processInput(model)
    }
}
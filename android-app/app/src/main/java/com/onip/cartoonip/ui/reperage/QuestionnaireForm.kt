package com.onip.cartoonip.ui.reperage

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.onip.cartoonip.data.QItem
import com.onip.cartoonip.data.SiteQuestionnaire

/**
 * Questions d'une section du questionnaire terrain, dessinées selon leur type (cf.
 * questionnaire-site.json). Les questions sautées (selon Q1 ou une réponse précédente) ne sont
 * pas affichées ; leurs réponses éventuelles sont conservées.
 */
@Composable
internal fun QuestionnaireItems(
    items: List<QItem>,
    answers: Map<String, String>,
    onAnswer: (String, String) -> Unit,
) {
    items.forEach { item ->
        if (!SiteQuestionnaire.visible(item, answers)) return@forEach
        val value = answers[item.id].orEmpty()
        when (item.type) {
            "note" -> Text(
                item.displayLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            "choice" -> {
                QuestionLabel(item.displayLabel)
                val legacy = item.legacyOptions.orEmpty().filter { it[0] == value }.map { it[0] to it[1] }
                ChoiceGroup(item.choices + legacy, value.ifEmpty { null }) { onAnswer(item.id, it.orEmpty()) }
                Precision(item, answers, onAnswer)
            }
            "yesno" -> YesNoRow(
                item.displayLabel,
                when (value) { "OUI" -> true; "NON" -> false; else -> null },
            ) { onAnswer(item.id, when (it) { true -> "OUI"; false -> "NON"; null -> "" }) }
            "multi" -> {
                QuestionLabel(item.displayLabel)
                val selected = value.split('|').filter { it.isNotBlank() }
                item.choices.forEach { (code, text) ->
                    val checked = code in selected
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .toggleable(value = checked, onValueChange = { on ->
                                val next = if (on) selected + code else selected - code
                                // Ordre du questionnaire, quel que soit l'ordre des clics.
                                onAnswer(item.id, item.choices.map { it.first }.filter { it in next }.joinToString("|"))
                            })
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.scale(0.8f))
                        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Precision(item, answers, onAnswer)
            }
            "grid" -> {
                QuestionLabel(item.displayLabel)
                item.rows.orEmpty().forEach { row ->
                    val key = item.rowKey(row[0])
                    Text(row[1], style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                    HorizontalChoice(
                        options = item.columns.orEmpty().map { it[0] to it[1] },
                        selected = answers[key],
                        onSelect = { onAnswer(key, it.orEmpty()) },
                    )
                }
                Spacer(Modifier.padding(bottom = 6.dp))
            }
            "longtext" -> FormField(item.displayLabel, value, { onAnswer(item.id, it.uppercase()) }, singleLine = false)
            "number" -> FormField(item.displayLabel, value, { onAnswer(item.id, it.filterDecimal()) },
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal), capitalize = false)
            "integer" -> FormField(item.displayLabel, value, { onAnswer(item.id, it.filterDigits()) },
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Number), capitalize = false)
            "phone" -> FormField(item.displayLabel, value, { onAnswer(item.id, it) },
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Phone), capitalize = false)
            "email" -> FormField(item.displayLabel, value, { onAnswer(item.id, it.trim()) },
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Email), capitalize = false)
            "date" -> OutlinedTextField(
                // Sélection forcée en fin de texte : le formatage auto (ajout des "-") réécrit la
                // chaîne à chaque frappe (même correctif que CaptureScreen).
                value = TextFieldValue(value, TextRange(value.length)),
                onValueChange = { onAnswer(item.id, formatIsoDateDigits(it.text)) },
                label = { Text("${item.displayLabel} (AAAA-MM-JJ)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            else -> FormField(item.displayLabel, value, { onAnswer(item.id, it.uppercase()) })
        }
    }
}

@Composable
private fun QuestionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

// Champ « Préciser » d'un choix « Autre (à préciser) », affiché seulement pour ces choix.
@Composable
private fun Precision(item: QItem, answers: Map<String, String>, onAnswer: (String, String) -> Unit) {
    if (!item.hasPrecision) return
    val chosen = answers[item.id].orEmpty().split('|')
    if (chosen.none { it in item.precise.orEmpty() }) return
    FormField("Préciser", answers[item.precisionKey].orEmpty(), { onAnswer(item.precisionKey, it.uppercase()) })
}

// Choix sur une ligne (colonnes d'un tableau : Bon / Moyen / Mauvais..., Oui / Non).
@Composable
private fun HorizontalChoice(options: List<Pair<String, String>>, selected: String?, onSelect: (String?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { (code, text) ->
            Row(
                modifier = Modifier
                    .selectable(selected = selected == code, onClick = { onSelect(if (selected == code) null else code) })
                    .padding(end = 12.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == code, onClick = null, modifier = Modifier.scale(0.75f))
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(2.dp))
        }
    }
}

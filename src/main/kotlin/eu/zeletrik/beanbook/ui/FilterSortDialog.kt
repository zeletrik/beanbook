package eu.zeletrik.beanbook.ui

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.checkbox.CheckboxGroup
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H4
import com.vaadin.flow.component.html.Hr
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.radiobutton.RadioButtonGroup
import com.vaadin.flow.component.select.Select
import eu.zeletrik.beanbook.beans.BagState
import eu.zeletrik.beanbook.beans.Process
import eu.zeletrik.beanbook.beans.RoastLevel

class FilterSortDialog(
    private val onApply: (FilterState) -> Unit,
) : Dialog() {

    private val sortGroup = RadioButtonGroup<SortField>().apply {
        setItems(*SortField.entries.toTypedArray())
        setItemLabelGenerator { it.label }
    }

    private val directionBtn = Button("↓ Newest first") {
        toggleDirection()
    }
    private var ascending = false

    private val roastGroup = CheckboxGroup<RoastLevel>().apply {
        label = "Roast level"
        setItems(*RoastLevel.entries.toTypedArray())
        setItemLabelGenerator { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
    }

    private val processGroup = CheckboxGroup<Process>().apply {
        label = "Process"
        setItems(*Process.entries.toTypedArray())
        setItemLabelGenerator { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
    }

    private val stateGroup = CheckboxGroup<BagState>().apply {
        label = "State"
        setItems(*BagState.entries.toTypedArray())
        setItemLabelGenerator { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
    }

    // 0 = "Any rating" sentinel (Select can't hold null items in Vaadin 25)
    private val ratingSelect = Select<Int>().apply {
        label = "Minimum rating"
        setItems(0, 1, 2, 3, 4, 5)
        setItemLabelGenerator { r -> if (r == 0) "Any rating" else r.toStars() }
        value = 0
    }

    init {
        setHeaderTitle("Filter & Sort")
        isResizable = false

        val content = VerticalLayout().apply {
            isPadding = true
            isSpacing = true
            style["min-width"] = "280px"

            add(H4("Sort by").apply { style["margin"] = "0" })
            add(sortGroup)

            add(HorizontalLayout(Span("Direction:"), directionBtn).apply {
                isSpacing = true
                style["align-items"] = "center"
            })

            add(Hr())
            add(H4("Filters").apply { style["margin"] = "0" })
            add(roastGroup)
            add(processGroup)
            add(stateGroup)
            add(ratingSelect)
        }

        val resetBtn = Button("Reset") {
            loadState(FilterState())
        }.apply { addThemeVariants(ButtonVariant.LUMO_TERTIARY) }

        val applyBtn = Button("Apply") {
            onApply(currentState())
            close()
        }.apply { addThemeVariants(ButtonVariant.LUMO_PRIMARY) }

        add(content)
        footer.add(resetBtn, applyBtn)
    }

    fun openWith(state: FilterState) {
        loadState(state)
        open()
    }

    private fun loadState(state: FilterState) {
        sortGroup.value = state.sortBy
        ascending = state.ascending
        updateDirectionLabel()
        roastGroup.value = state.roastLevels
        processGroup.value = state.processes
        stateGroup.value = state.bagStates
        ratingSelect.value = state.minRating ?: 0
    }

    private fun currentState() = FilterState(
        sortBy = sortGroup.value ?: SortField.PURCHASE_DATE,
        ascending = ascending,
        roastLevels = roastGroup.value ?: emptySet(),
        processes = processGroup.value ?: emptySet(),
        bagStates = stateGroup.value ?: emptySet(),
        minRating = ratingSelect.value.takeIf { it > 0 },
    )

    private fun toggleDirection() {
        ascending = !ascending
        updateDirectionLabel()
    }

    private fun updateDirectionLabel() {
        directionBtn.text = if (ascending) "↑ Oldest/lowest first" else "↓ Newest/highest first"
    }
}

<script setup lang="ts">
import * as d3 from 'd3';
import CardPreview from '@/components/deck/CardPreview.vue';
import type { DeckCard } from '@/api/message';
import { computed, onMounted, ref, watch } from 'vue';

type GroupKeys = 'type' | 'manaValue' | 'manaCost';

const props = defineProps<{ cards: DeckCard[], width: number, height: number }>();

onMounted(() => {
  watch(() => props.cards, () => {
    buildCharts();
  }, { immediate: true });
});

function buildCharts() {
  buildChart('#card-count-chart', 'type', 'Card Type');
  buildChart('#mana-value-chart', 'manaValue', 'Mana Value');
  const groupedData = groupByManaCost(props.cards);

  d3.select("#color-pie-chart").selectAll("*").remove(); // Clear previous content
  const svg = d3.select("#color-pie-chart").append("svg")
    .attr("width", props.width)
    .attr("height", props.height)
    .attr("class", "d3-chart");
  buildSummedPieChart(svg, props.width, props.height, groupedData);
}

function buildChart(containerId: string, key: GroupKeys, xAxisLabel: string) {
  d3.select(containerId).selectAll("*").remove(); // Clear previous content
  const groupedData = groupByKey(props.cards, key);
  const svg = d3.select(containerId).append("svg")
    .attr("width", props.width)
    .attr("height", props.height)
    .attr("class", "d3-chart")
  // .attr("viewBox", "0 0 800 600")
  // .style("background-color", "pink");
  buildSummedBarChart(svg, props.width, props.height, 50, 50, groupedData, xAxisLabel, key);
}


function buildSummedBarChart(svg: d3.Selection<SVGSVGElement, unknown, HTMLElement, any>, width: number, height: number, graphWidthOffset: number, graphHeightOffset: number, data: { [key: string]: number }, xAxisLabel: string, key: GroupKeys = 'type') {
  const graphPadding = 10;
  const graphHeight = height - graphHeightOffset - graphPadding; // Reserve space for axes
  const graphWidth = width - graphWidthOffset - graphPadding; // Reserve space for axes

  let xAxisDomain = Object.keys(data);
  if (xAxisDomain.every(d => !isNaN(Number(d)))) {
    const maxDomainValue = d3.max(xAxisDomain.map(Number)) ?? 0;
    xAxisDomain = d3.range(0, maxDomainValue + 1).map(String);
  } else {
    // Sort alphabetically if not all numeric
    xAxisDomain.sort();
  }
  const xScale = d3.scaleBand()
    .domain(xAxisDomain)
    .range([width - graphWidth, width - graphPadding])
    .padding(0.1);

  const yScale = d3.scaleLinear()
    .domain([0, d3.max(Object.values(data)) ?? 0])
    .nice()
    .range([graphHeight, graphPadding]);

  svg.selectAll(".bar")
    .data(Object.entries(data))
    .enter().append("rect")
    .attr("class", "bar")
    .attr("x", d => xScale(d[0])!)
    .attr("y", d => yScale(d[1]))
    .attr("width", xScale.bandwidth())
    .attr("height", d => graphHeight - yScale(d[1]))
    .attr("fill", "rgb(78, 128, 220)")
    .style("cursor", "pointer")
    .on("mouseover", function (event, d) {
      const intenseColor = d3.rgb("rgb(78, 128, 220)").brighter(0.2).toString();
      d3.select(this)
        .attr("fill", intenseColor); // Change color on hover
      const tooltip = d3.select("body").append("div")
        .attr("class", "tooltip")
        .style("position", "absolute")
        .style("background-color", "white")
        .style("border", "1px solid black")
        .style("pointer-events", "none")
        .style("padding", "5px")
        .html(`${d[0]}: ${d[1]} cards`);
      tooltip.style("left", (event.pageX + 5) + "px")
        .style("top", (event.pageY - 28) + "px");
    })
    .on("mousemove", function (event) {
      d3.select(".tooltip")
        .style("left", (event.pageX + 5) + "px")
        .style("top", (event.pageY - 28) + "px");
    })
    .on("mouseout", function () {
      d3.select(this)
        .attr("fill", "rgb(78, 128, 220)"); // Reset color on mouse out
      d3.select(".tooltip").remove();
    })
    .on("click", function (event, d) {
      // Handle click event if needed
      console.log(`Clicked on ${d[0]}: ${d[1]}`);
      processBarClick(d[0], key);
    });

  // Add axes
  const xAxisGroup = svg.append("g")
    .attr("class", "x-axis")
    .attr("transform", `translate(0, ${graphHeight})`)
    .call(d3.axisBottom(xScale));
  // xAxisGroup.selectAll("text")
  //     .style("text-anchor", "middle") // Align text to the end for rotation
  //     .attr("dx", "-.8em") // Adjust horizontal position
  //     .attr("dy", ".15em") // Adjust vertical position
  //     .attr("transform", "rotate(-45)"); // Rotate by -45 degrees
  svg.append("g")
    .attr("class", "y-axis")
    .attr("transform", `translate(${width - graphWidth}, 0)`)
    .call(d3.axisLeft(yScale));
  svg.append("text")
    .attr("class", "x-axis-label")
    .attr("x", (graphWidth + graphWidthOffset * 2) / 2)
    .attr("y", graphHeight + 35)
    .style("text-anchor", "middle")
    .text(xAxisLabel);
  svg.append("text")
    .attr("class", "y-axis-label")
    .attr("transform", "rotate(-90)")
    .attr("x", -(height - graphHeightOffset) / 2)
    .attr("y", 20)
    .style("text-anchor", "middle")
    .text("Card Count");
}


const colors = {
  "W": "rgb(236, 217, 191)", // White
  "U": "rgb(22, 90, 199)", // Blue
  "B": "rgb(66, 66, 66)", // Black
  "R": "rgb(211, 52, 52)", // Red
  "G": "rgb(49, 192, 49)", // Green
  "C": "rgb(120, 133, 129)", // Colorless
}

function buildSummedPieChart(svg: d3.Selection<SVGSVGElement, unknown, HTMLElement, any>, width: number, height: number, data: { [key: string]: number }) {
  const radius = Math.min(width, height) / 2 - 10;
  const color = (c: string) => colors[c as keyof typeof colors];
  const pie = d3.pie<[string, number]>()
    .value(d => d[1])
    .sort(null);
  const arc = d3.arc()
    .innerRadius(0)
    .outerRadius(radius);
  const arcs = pie(Object.entries(data));
  svg.attr("width", width)
    .attr("height", height)
    .append("g")
    .attr("transform", `translate(${width / 2}, ${height / 2})`)
    .selectAll("path")
    .data(arcs)
    .enter().append("path")
    .attr("d", arc as any)
    .attr("fill", (d: d3.PieArcDatum<[string, number]>) => color(d.data[0]))
    .attr("stroke", "white")
    .style("stroke-width", "2px")
    .on("mouseover", function (event, d: d3.PieArcDatum<[string, number]>) {
      const intenseColor = d3.rgb(color(d.data[0])).brighter(0.2).toString();
      d3.select(this)
        .attr("fill", intenseColor); // Change color on hover
      const tooltip = d3.select("body").append("div")
        .attr("class", "tooltip")
        .style("position", "absolute")
        .style("background-color", "white")
        .style("border", "1px solid black")
        .style("pointer-events", "none")
        .style("padding", "5px")
        .html(`${d.data[0]}: ${d.data[1]}`);
      tooltip.style("left", (event.pageX + 5) + "px")
        .style("top", (event.pageY - 28) + "px");
    })
    .on("mousemove", function (event) {
      d3.select(".tooltip")
        .style("left", (event.pageX + 5) + "px")
        .style("top", (event.pageY - 28) + "px");
    })
    .on("mouseout", function () {
      d3.select(this)
        .attr("fill", (d: any) => color(d.data[0])); // Reset color on mouse out
      d3.select(".tooltip").remove();
    })
    .on("click", function (event, d: d3.PieArcDatum<[string, number]>) {
      // Handle click event if needed
      console.log(`Clicked on ${d.data[0]}: ${d.data[1]}`);
    });
}

function groupByKey(data: DeckCard[], key: GroupKeys) {
  const result: { [key: string]: number } = {};
  for (const card of data) {
    if (result[card[key]!] === undefined) {
      result[card[key]!] = 0;
    }
    result[card[key]!] += card.count;
  }
  return result;
}

function groupByManaCost(data: DeckCard[]) {
  const result: { [key: string]: number } = {};
  for (const card of data) {
    const manaCost = card.manaCost;
    if (manaCost) {
      const manaSymbols = getManaSymbols(manaCost);
      for (const sym of manaSymbols) {
        if (result[sym] === undefined) {
          result[sym] = 0;
        }
        result[sym] += card.count;
      }
    }
  }
  return result;
}

function getManaSymbols(manaCost: string) {
  if (!manaCost) return [];
  const manaSymbols = [];
  const regex = /{([WUBRGC]+)}/g;
  let match;
  while ((match = regex.exec(manaCost)) !== null) {
    manaSymbols.push(match[1]);
  }
  return manaSymbols;
}

function processBarClick(bar: string, key: GroupKeys) {
  if (key === 'manaValue') {
    showManaValueBreakdown.value = true;
    selectedManaValue.value = Number(bar);
  } else if (key === 'type') {
    showCardTypeBreakdown.value = true;
    selectedCardType.value = bar;
  }
}

const showManaValueBreakdown = ref(false);
const selectedManaValue = ref<number | null>(null);
const mvCards = computed(() => {
  if (selectedManaValue.value !== null) {
    return props.cards.filter(card => card.manaValue === selectedManaValue.value);
  }
  return [];
});

const showCardTypeBreakdown = ref(false);
const selectedCardType = ref<string | null>(null);
const cardTypeCards = computed(() => {
  if (selectedCardType.value !== null) {
    return props.cards.filter(card => card.type === selectedCardType.value);
  }
  return [];
});

function getCardTypeDetails(key: 'subTypes' | 'superTypes') {
  const details: { [key: string]: { [key: string]: number } } = {};
  for (const card of props.cards) {
    if (!details[card.type]) {
      details[card.type] = {};
    }
    for (const type of card[key] ?? []) {
      if (!details[card.type][type]) {
        details[card.type][type] = 0;
      }
      details[card.type][type] += card.count;
    }
  }

  // delete all empty types 
  for (const type in details) {
    if (Object.keys(details[type]).length === 0) {
      delete details[type];
    }
  }
  return details;
}

const cardSubTypeDetails = computed(() => getCardTypeDetails('subTypes'));
const cardSuperTypeDetails = computed(() => getCardTypeDetails('superTypes'));

</script>

<template>
  <div id="decklist-stats">
    <div class="viz-container">
      <h2>Mana Value Breakdown</h2>
      <div class="viz-chart">
        <div id="mana-value-chart"></div>
        <div v-if="showManaValueBreakdown" class="viz-cards">
          <h3>Mana value {{ selectedManaValue }}</h3>
          <card-preview v-for="card in mvCards" :key="card.id" :card="card" :show-count="true" />
        </div>
      </div>
    </div>
    <div class="viz-container">
      <h2>Card Type Breakdown</h2>
      <div class="viz-chart">
        <div id="card-count-chart"></div>
        <div v-if="showCardTypeBreakdown" class="viz-cards">
          <h3>{{ selectedCardType }}</h3>
          <card-preview v-for="card in cardTypeCards" :key="card.id" :card="card" :show-count="true" />
        </div>
      </div>
    </div>
    <div class="viz-container">
      <h2>Card Type Details</h2>
      <div class="card-type-details-container">
        <div class="card-type-details">
          <h3>Subtypes</h3>
          <div>
            <div class="card-type-details-section" v-for="(subtypes, type) in cardSubTypeDetails" :key="type">
              <h4>{{ type }}</h4>
              <ul class="card-type-detail-list">
                <li class="card-type-detail-list-element" v-for="(count, subtype) in subtypes" :key="subtype">
                  {{ count }} {{ subtype }}
                </li>
              </ul>
            </div>
          </div>
        </div>
        <div class="card-type-details">
          <h3>Supertypes</h3>
          <div>
            <div class="card-type-details-section" v-for="(subtypes, type) in cardSuperTypeDetails" :key="type">
              <h4>{{ type }}</h4>
              <ul class="card-type-detail-list">
                <li class="card-type-detail-list-element" v-for="(count, subtype) in subtypes" :key="subtype">
                  {{ count }} {{ subtype }}
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="viz-container">
      <h2>Color Breakdown</h2>
      <div class="viz-chart">
        <div id="color-pie-chart"></div>
      </div>
    </div>
  </div>
</template>


<style scoped>
.viz-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-wrap: wrap;
  justify-content: center;
  background-color: #eee;
  border: 1px solid #ddd;
  border-radius: 5px;
  max-width: 80%;
  width: 100%;
  margin: 20px auto;
  padding: 1em 2em;
  min-width: 250px;
}

.viz-chart {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  width: 100%;
  gap: 20px;
}

.viz-cards {
  display: flex;
  flex-direction: column;
  max-height: 100%;
  overflow-y: auto;
  background-color: white;
  border: 1px solid #ddd;
  border-radius: 5px;
  padding: 30px 10px;
  min-width: 250px;
}

.viz-cards h3 {
  margin-top: 0;
  padding: 0;
  text-align: center;
}

.card-type-details-container {
  display: flex;
  flex-direction: row;
  gap: 20px;
  align-items: baseline;
  justify-content: space-around;
  width: 100%;
}

.card-type-details {
  display: flex;
  flex-direction: column;
  flex-wrap: wrap;
  justify-content: center;
  gap: 20px;
  box-sizing: border-box;
  max-width: 50%;
}

.card-type-details>div {
  display: flex;
  flex-direction: row;
  background-color: #f9f9f9;
  border: 1px solid #ddd;
  border-radius: 5px;
  padding: 10px;
  width: 100%;
  gap: 20px;
  box-sizing: border-box;
  flex-wrap: wrap;
}

.card-type-detail-list {
  padding-left: 15px;
  padding-right: 15px;
}

.card-type-detail-list-element {
  list-style: none;
}
</style>

<style>
svg.d3-chart {
  background-color: white;
  border-radius: 5px;
  border: 1px solid #ddd;
  padding: 5px;
}
</style>
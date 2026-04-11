import { computed, ref } from 'vue';

export function useCardHover() {
    const display = ref('none');
    const left = ref(0);
    const top = ref(0);
    const cardImage = ref<HTMLImageElement>();

    const hoverStyle = computed(() => ({
        display: display.value,
        left: `${left.value}px`,
        top: `${top.value}px`,
    }));

    function normalizePreviewY(e: MouseEvent) {
        const height = (cardImage.value?.height ?? 0) + 20;
        let newY = e.pageY;
        if (newY + height > window.innerHeight + window.scrollY) {
            newY = window.innerHeight + window.scrollY - height;
        }
        return newY;
    }

    function normalizePreviewX(e: MouseEvent) {
        const width = (cardImage.value?.width ?? 0) + 20;
        let newX = e.pageX;
        if (newX + width > window.innerWidth + window.scrollX) {
            newX = window.innerWidth + window.scrollX - width;
        }
        return newX;
    }

    function mouseOver(e: MouseEvent) {
        display.value = 'inline';
        left.value = normalizePreviewX(e);
        top.value = normalizePreviewY(e);
    }

    function mouseMove(e: MouseEvent) {
        left.value = normalizePreviewX(e);
        top.value = normalizePreviewY(e);
    }

    function mouseLeave() {
        display.value = 'none';
    }

    return { cardImage, hoverStyle, mouseOver, mouseMove, mouseLeave };
}

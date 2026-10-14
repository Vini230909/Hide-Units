package hideunits;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.entities.abilities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static arc.Core.*;
import static mindustry.Vars.*;

/**
 * Hides units near the configured anchor point.
 *
 * Each frame, affected units are temporarily removed from {@link Groups#draw} before the
 * vanilla renderer iterates it, and put back right after rendering ({@link Trigger#postDraw}).
 * If the opacity setting is above zero, the removed units are drawn manually into a
 * framebuffer which is then blitted over the screen with the chosen alpha — the same
 * capture-and-blit pattern the vanilla renderer uses for animated shields.
 */
public class HideUnitsRenderer{
    /** Z layer the faded units are drawn at: above flying units, below the overlay UI. */
    public static final float fadeLayer = Layer.flyingUnit + 1f;

    /** Units removed from the draw group this frame. */
    private final Seq<Unit> faded = new Seq<>();
    /** Quasar forcefields are drawn separately at their normal shield layer. */
    private final Seq<Unit> quasarUnits = new Seq<>();
    private final Seq<ForceFieldAbility> quasarShields = new Seq<>();
    /** Anchor points active this frame; more than one in "all" mode. */
    private final Seq<Vec2> anchors = new Seq<>();
    private final Vec2 playerAnchor = new Vec2(), cursorAnchor = new Vec2(), cameraAnchor = new Vec2();
    private final IntSeq suppressedIndices = new IntSeq();
    private final Seq<Ability> suppressedAbilities = new Seq<>();
    private final Ability noDrawAbility = new Ability(){};

    private FrameBuffer buffer;
    private FrameBuffer shieldBuffer;
    private AlphaShader shader;

    public HideUnitsRenderer(){
        Events.run(Trigger.draw, this::draw);
        Events.run(Trigger.postDraw, this::restore);
    }

    void draw(){
        //if the previous frame ended abnormally and postDraw never ran, put units back now
        restore();
        quasarUnits.clear();
        quasarShields.clear();

        if(!HideUnitsSettings.enabled() || !state.isGame()) return;

        findAnchors();
        if(anchors.isEmpty()) return;

        float range = HideUnitsSettings.range() * tilesize;
        Unit ownUnit = player.unit();

        Groups.unit.each(unit ->
            unit != ownUnit
            && HideUnitsSettings.affects(unit.type)
            && withinAnyAnchor(unit, range),
        faded::add);

        if(faded.isEmpty()) return;

        for(Unit unit : faded){
            if(unit.type.name.equals("quasar")){
                for(Ability ability : unit.abilities){
                    if(ability instanceof ForceFieldAbility shield){
                        quasarUnits.add(unit);
                        quasarShields.add(shield);
                    }
                }
            }
        }

        for(Unit unit : faded){
            Groups.draw.remove(unit);
        }

        float opacity = HideUnitsSettings.opacity();
        if(opacity <= 0.001f) return;

        if(!quasarShields.isEmpty()){
            Draw.draw(Layer.shields, () -> drawQuasarShields(opacity));
        }

        if(buffer == null){
            buffer = new FrameBuffer();
            shader = new AlphaShader();
        }
        buffer.resize(graphics.getWidth(), graphics.getHeight());

        float prevZ = Draw.z();
        Draw.draw(fadeLayer, () -> {
            buffer.begin(Color.clear);
            Draw.reset();
            for(Unit unit : faded){
                drawUnitWithoutQuasarShield(unit);
            }
            Draw.reset();
            buffer.end();

            shader.alpha = opacity;
            buffer.blit(shader);
        });
        Draw.z(prevZ);
    }

    private void drawUnitWithoutQuasarShield(Unit unit){
        if(!unit.type.name.equals("quasar")){
            unit.draw();
            return;
        }

        Ability[] abilities = unit.abilities;
        suppressedIndices.clear();
        suppressedAbilities.clear();

        for(int i = 0; i < abilities.length; i++){
            if(abilities[i] instanceof ForceFieldAbility){
                suppressedIndices.add(i);
                suppressedAbilities.add(abilities[i]);
                abilities[i] = noDrawAbility;
            }
        }

        try{
            unit.draw();
        }finally{
            for(int i = 0; i < suppressedIndices.size; i++){
                abilities[suppressedIndices.get(i)] = suppressedAbilities.get(i);
            }
            suppressedIndices.clear();
            suppressedAbilities.clear();
        }
    }

    private void drawQuasarShields(float opacity){
        if(opacity >= 0.999f){
            drawQuasarShields();
            return;
        }

        if(shieldBuffer == null){
            shieldBuffer = new FrameBuffer();
        }
        shieldBuffer.resize(graphics.getWidth(), graphics.getHeight());
        shieldBuffer.begin(Color.clear);
        Draw.reset();
        drawQuasarShields();
        Draw.reset();
        shieldBuffer.end();

        float prevZ = Draw.z();
        shader.alpha = opacity;
        shader.unpremultiply = true;
        try{
            shieldBuffer.blit(shader);
        }finally{
            shader.unpremultiply = false;
            Draw.z(prevZ);
        }
    }

    private void drawQuasarShields(){
        for(int i = 0; i < quasarShields.size; i++){
            Draw.reset();
            quasarShields.get(i).draw(quasarUnits.get(i));
        }
        Draw.reset();
    }

    void restore(){
        for(Unit unit : faded){
            //skip units that were removed from the world in the (abnormal) case where
            //restoration happens a frame late
            if(unit.isAdded()){
                Groups.draw.add(unit);
            }
        }
        faded.clear();
    }

    /** Collects the anchor points active for the current mode into {@link #anchors}. */
    private void findAnchors(){
        anchors.clear();
        int mode = HideUnitsSettings.mode();

        if(mode == HideUnitsSettings.modePlayer || mode == HideUnitsSettings.modeAll){
            Unit unit = player.unit();
            if(unit != null && !unit.dead && unit.isAdded()){
                anchors.add(playerAnchor.set(unit.x, unit.y));
            }
        }
        if(mode == HideUnitsSettings.modeCursor || mode == HideUnitsSettings.modeAll){
            anchors.add(cursorAnchor.set(input.mouseWorld()));
        }
        if(mode == HideUnitsSettings.modeCamera || mode == HideUnitsSettings.modeAll){
            anchors.add(cameraAnchor.set(camera.position));
        }
    }

    private boolean withinAnyAnchor(Unit unit, float range){
        for(Vec2 anchor : anchors){
            if(unit.within(anchor.x, anchor.y, range)) return true;
        }
        return false;
    }

    /** Draws a fullscreen buffer multiplied by a constant alpha. */
    static class AlphaShader extends Shader{
        public float alpha = 1f;
        public boolean unpremultiply;

        AlphaShader(){
            super("""
            attribute vec4 a_position;
            attribute vec2 a_texCoord0;

            varying vec2 v_texCoords;

            void main(){
                v_texCoords = a_texCoord0;
                gl_Position = a_position;
            }
            """, """
            uniform sampler2D u_texture;
            uniform float u_alpha;
            uniform float u_unpremultiply;

            varying vec2 v_texCoords;

            void main(){
                vec4 color = texture2D(u_texture, v_texCoords);
                vec3 rgb = u_unpremultiply > 0.5 ? color.rgb / max(color.a, 0.0001) : color.rgb;
                gl_FragColor = vec4(rgb, color.a * u_alpha);
            }
            """);
        }

        @Override
        public void apply(){
            setUniformf("u_alpha", alpha);
            setUniformf("u_unpremultiply", unpremultiply ? 1f : 0f);
        }
    }
}

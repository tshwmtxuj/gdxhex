package untra.player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import com.badlogic.gdx.utils.XmlReader.Element;
import com.badlogic.gdx.utils.XmlWriter;

import untra.database.Animation;
import untra.database.Armor;
import untra.database.Armor_Type;
import untra.database.Database;
import untra.database.Elemental;
import untra.database.Klass;
import untra.database.Race;
import untra.database.Skill;
import untra.database.Skill_Type;
import untra.database.Status;
import untra.database.Weapon;
import untra.driver.IXml;

public class Actor implements IXml<Actor> {
	public boolean _initialized;
	public boolean playable_character;
	public String Name;
	public Klass cclass;
	// public boolean is_male;
	public int LEVEL;
	public int EXP;
	// public int N_EXP;
	// public boolean is_monster;

	// public Item[] items;
	public ArrayList<Skill> skills;

	public Weapon weapon = new Weapon();
	public Armor armor = new Armor();

	/**
	 * 0 - ATK, 1 - DEF, 2 - SPD, 3 - ACC, 4 - EVA
	 */
	public int[] stat_modifiers = new int[5];
	public ArrayList<Status> states;
	public int HP;
	public int SP;
	// private Skill_Type _damage_factor;

	public ArrayList<Elemental> elementals;
	private int _maxhp;
	// private int _maxhp_plus;
	private int _maxsp;
	// private int _maxsp_plus;
	private int _MOV;
	private int _VSN;
	private int _SPD;
	private int _ATK;
	private int _DEF;
	private int _POW;
	private int _SKL;
	private int _MND;

	/**
	 * returns the amount of exp needed to progress from level n-1 tot level n
	 * 
	 * @param n
	 * @return
	 */
	private int expToLevel(int n) {
		return n * (n + 10);
	}

	private Race _race = Race.Human;

	// TODO finish
	public Race getRace() {
		return cclass.race;
	}

	/**
	 * Returns a float from 0.0 to 1.0 representing the ratio of HP to Maximum
	 * HP
	 * 
	 * @return rate
	 */
	public float HP_rate() {
		return ((float) this.HP / (float) this.MAX_HP());
	}

	/**
	 * Returns a float from 0.0 to 1.0 representing the ratio of SP to Maximum
	 * SP
	 * 
	 * @return rate
	 */
	public float SP_rate() {
		return ((float) this.SP / (float) this.MAX_SP());
	}

	/**
	 * returns true if the battler cannot move due to status
	 * 
	 * @return
	 */
	public boolean status_cannot_move() {
		for (Status s : states) {
			if (s.disable_move)
				return true;
		}
		return false;
	}

	/**
	 * returns true if the battler cannot use special moves due to status
	 * 
	 * @return
	 */
	public boolean status_cannot_special() {
		for (Status s : states) {
			if (s.disable_skills)
				return true;
		}
		return false;
	}

	/**
	 * returns true if the battler cannot act due to status
	 * 
	 * @return
	 */
	public boolean status_cannot_act() {
		for (Status s : states) {
			if (s.cannot_act)
				return true;
		}
		return false;
	}

	/**
	 * returns true if the battler cannot use spells
	 * 
	 * @return
	 */
	public boolean status_cannot_cast() {
		for (Status s : states) {
			if (s.disable_magic)
				return true;
		}
		return false;
	}

	/**
	 * returns true if the battler cannot evade attacks
	 * 
	 * @return
	 */
	public boolean status_cannot_evade() {
		for (Status s : states) {
			if (s.is_cant_evade)
				return true;
		}
		return false;
	}

	public boolean status_is_confused() {
		if (status_is_charmed() && status_is_berserk())
			return true;
		return false;
	}

	public boolean status_is_charmed() {
		for (Status s : states) {
			if (s.always_target_allies)
				return true;
		}
		return false;
	}

	public boolean status_is_berserk() {
		for (Status s : states) {
			if (s.always_target_enemies)
				return true;
		}
		return false;
	}

	public Animation use_anim() {
		if (weapon != null)
			return Database.animations[weapon.use_anim_id];
		else
			return Database.animations[0];
	}

	public Animation hit_anim() {
		if (weapon != null)
			return Database.animations[weapon.hit_anim_id];
		else
			return Database.animations[0];
	}

	public int MAX_HP() {

		int n = (_maxhp + _maxhp);
		n += armor.HPP;
		n = Math.max(Math.min(n, 999), 1);
		return (int) n;
	}

	public void changeMAX_HP(int value) {
		_maxhp += value;
		_maxhp = Math.max(Math.min(_maxhp, 999), -999);
		HP = Math.min(HP, _maxhp);
	}

	public int MAX_SP() {
		int n = (_maxsp);
		n += armor.SPP;
		n = Math.max(Math.min(n, 999), 1);
		return (int) n;
	}

	public void changeMAX_SP(int value) {
		_maxsp += value;
		_maxsp = Math.max(Math.min(_maxsp, 999), -999);
		SP = Math.min(SP, _maxsp);
	}

	public int ATK() {
		int s = 1;
		s += (cclass.bonus(8) * 5);
		s += _ATK;
		s += weapon.ATK;
		s += (getRace() == Race.Emberborn || getRace() == Race.Undead) ? 4 : 0;
		s += (getRace() == Race.Fey) ? -2 : 0;
		float f = 1.0f;
		for (Status u : states) {
			f *= u.ATK_Modifier;
		}
		s *= f;
		s = Math.max(Math.min(s, 255), 1);
		// this modifier goes at the end, and applies battle imposed nerf/buffs
		// to the stat
		s *= get_atk_modifier();
		return s;
	}

	public int DEF() {
		int s = 1;
		s += (cclass.bonus(9) * 5);
		s += _DEF;
		s += armor.DEF;
		s += (getRace() == Race.Emberborn || getRace() == Race.Undead
				|| getRace() == Race.Aquatic || getRace() == Race.Beast) ? -2
				: 0;
		// if (armor.HasValue) s += armor.Value.DEF;
		s = Math.max(Math.min(s, 255), 1);
		float f = 1.0f;
		for (Status u : states) {
			f *= (int) u.DEF_Modifier;
		}
		s *= f;
		s = Math.max(Math.min(s, 255), 1);
		// this modifier goes at the end, and applies battle imposed nerf/buffs
		// to the stat
		s *= get_def_modifier();
		return (int) s;
	}

	public int POW() {
		int s = _POW;
		s += weapon.POW;
		s += armor.POW;
		float f = 1.0f;
		for (Status u : states) {
			s *= (int) u.POW_Modifier;
		}
		s *= f;
		s = Math.max(Math.min(s, 255), 1);
		return (int) s;
	}

	public void changePOW(int value) {
		_POW += value;
		_POW = Math.max(Math.min(_POW, 255), -255);
	}

	public int SKL() {
		int s = _SKL;
		s += weapon.SKL;
		s += armor.SKL;
		float f = 1.0f;
		for (Status u : states) {
			f *= (int) u.SKL_Modifier;
		}
		s *= f;
		s = Math.max(Math.min(s, 255), 1);
		return (int) s;
	}

	public void changeSKL(int value) {
		_SKL += value;
		_SKL = Math.max(Math.min(_SKL, 255), -255);
	}

	public int MND() {

		int s = _MND;
		s += weapon.MND;
		s += armor.MND;
		float f = 1.0f;
		for (Status u : states) {
			f *= (int) u.MND_Modifier;
		}
		s *= f;
		s = Math.max(Math.min(s, 255), 1);
		return s;
	}

	public void changeMND(int value) {
		_MND += value;
		_MND = Math.max(Math.min(_MND, 255), -255);
	}

	public Actor(Klass c, int level) {
		// Temp
		skills = new ArrayList<Skill>();
		// items = new Item[6];
		// is_male = true;
		// _damage_factor = Skill_Type.POW;
		// End Temp
		Random temp_rand = new Random();
		if (level > 60 || level < 1)
			throw new IndexOutOfBoundsException("level value out of bounds)");
		Name = "TEMPORARY";
		cclass = c;
		LEVEL = level;
		_maxhp = 5;
		_maxsp = 4;

		_MOV = 0;
		_VSN = 0;
		_SPD = 0;

		_POW = 1;
		_SKL = 1;
		_MND = 1;
		EXP = 0;
		_SPD = 0;
		weapon = Database.weapons[0];
		// N_EXP = (48 * level);
		states = new ArrayList<Status>();
		// ABSORB_ELEMS = new ArrayList<Elemental>();
		// RESISTANT_ELEMS = new ArrayList<Elemental>();
		// IMMUNE_ELEMS = new ArrayList<Elemental>();
		// WEAK_ELEMS = new ArrayList<Elemental>();
		for (int u = 0; u < level; u++) {
			_maxhp += c.advance(0, temp_rand);
			_maxsp += c.advance(1, temp_rand);
			_POW += c.advance(2, temp_rand);
			_SKL += c.advance(3, temp_rand);
			_MND += c.advance(4, temp_rand);
			_SPD += c.advance(7, temp_rand);
		}
		// SP = _maxsp;
		// HP = _maxhp;
		_ATK = 0;
		_DEF = 0;
		// Monsters do not wield weapons, so their attack is automatically
		// generated.
		/*
		 * if (is_monster) { float temp = (float) (((c.bonus(8) * 100 + 200) +
		 * MathUtils.random( -15, 15)) / 1000); _atk_plus = (int) (temp *
		 * LEVEL); temp = (float) (((c.bonus(9) * 100 + 200) +
		 * MathUtils.random(-15, 15)) / 1000); _def_plus = (int) (temp * LEVEL);
		 * }
		 */
		// Sprites

		HP = this.MAX_HP();
		SP = this.MAX_SP();
		reset_stat_modifiers();
		_initialized = true;

	}

	public float getACC() {
		float n = 0.98f;
		for (Status s : states) {
			n *= (int) s.ACC_Modifier;
		}
		n += get_acc_modifier();
		return n;
	}

	// an attacks hits if a random number from 0 - 1 is greater than A.acc -
	// T.eva
	// by default there is a 1/20 chance an attack misses
	// with full evasion, there is only a 1/5 chance an attack lands

	public float getEVA() {
		float n = 0.03f;
		n += get_eva_modifier();
		return n;
	}

	public boolean is_monster() {
		return (getRace() == Race.Aquatic || getRace() == Race.Beast
				|| getRace() == Race.Fauna || getRace() == Race.Ignis || getRace() == Race.Undead);
	}

	public Skill_Type getdamage_factor() {
		if (is_monster())
			return Skill_Type.POW;
		else
			return weapon.damage_type();
	}

	public int MOV() {

		int n = (cclass.bonus(5) + _MOV + 3);
		n += weapon.MOV;
		n += armor.MOV;
		n += (getRace() == Race.Ratmen) ? 1 : 0;
		n += (getRace() == Race.Undead || getRace() == Race.Fauna) ? -1 : 0;
		float f = 1.0f;
		for (Status s : states) {
			f *= (int) s.MOV_Modifier;
		}
		n *= f;
		n = Math.max(Math.min(n, 16), 1);
		return (int) n;
	}

	public void changeMOV(int value) // These values should never be explicitly
										// set. The following code blocks are
										// untested.
	{
		_MOV += value;
		_MOV = Math.max(Math.min(_MOV, 16), -16);
	}

	public int getVSN() {
		int n = (cclass.bonus(6) + _VSN + 4);
		n += weapon.VSN;
		n += armor.VSN;
		n += (getRace() == Race.Ratmen) ? 1 : 0;
		n += (getRace() == Race.Undead) ? -1 : 0;
		n = Math.max(Math.min(n, 16), 1);
		for (Status s : states) {
			n *= (int) s.VSN_Modifier;
		}
		n = Math.max(Math.min(n, 255), 1);
		return (int) n;
	}

	public void changeVSN(int value)// These values should never be explicitly
									// set.
									// The following code blocks are untested.
	{
		_VSN += value;
		_VSN = Math.max(Math.min(_VSN, 16), -16);
	}

	/**
	 * Influences the frequency the character gets to move and make a turn;
	 * 
	 * @return
	 */
	public int getSPD() {
		int n = ((cclass.bonus(7) * 4) + _SPD + 12);
		n += weapon.SPD;
		n += armor.SPD;
		n -= armor.type == Armor_Type.medium ? 12 : 0;
		n -= armor.type == Armor_Type.heavy ? 24 : 0;
		n += (getRace() == Race.Emberborn || getRace() == Race.Undead || getRace() == Race.Fauna) ? -5
				: 0;
		n += (getRace() == Race.Avis || getRace() == Race.Ratmen || getRace() == Race.Canid) ? 5
				: 0;
		float f = 1.0f;
		for (Status s : states) {
			f *= (int) s.SPD_Modifier;
		}
		n *= f;
		n = Math.max(Math.min(n, 255), 1);
		n *= get_spd_modifier();
		return (int) n;
	}

	public void changeSPD(int value)// These values should never be explicitly
									// set.
									// The following code blocks are untested.
	{
		_SPD += value;
		_SPD = Math.max(Math.min(_SPD, 600), -600);

	}

	public boolean skill_can_use(Skill S) {
		if (this.SP < S.sp_cost)
			return false;
		for (Status status : states) {
			if (status.disable_magic && S.is_spell)
				return false;
			if (status.disable_skills && !S.is_spell)
				return false;
		}
		return true;
	}

	public void remove_auto_states(Random rand) {
		for (int i = states.size() - 1; i >= 0; i--) {
			if (states.get(i).auto_release(rand)) {
				states.remove(i);
			}
		}
	}

	@Override
	public void xmlWrite(XmlWriter xml) throws IOException {
		xml.element("Actor");
		xml.element("Name").text(Name).pop();
		xml.element("Class").text(cclass.id).pop();
		xml.element("PC").text(playable_character).pop();
		xml.element("LVL").text(LEVEL).pop();
		xml.element("EXP").text(EXP).pop();

		xml.element("HPP").text(_maxhp).pop();
		xml.element("SPP").text(_maxsp).pop();
		xml.element("HP").text(HP).pop();
		xml.element("SP").text(SP).pop();
		xml.element("POW").text(_POW).pop();
		xml.element("SKL").text(_SKL).pop();
		xml.element("MND").text(_MND).pop();
		xml.element("MOV").text(_MOV).pop();
		xml.element("VSN").text(_VSN).pop();
		xml.element("SPD").text(_SPD).pop();
		xml.element("ATK").text(_ATK).pop();
		xml.element("DEF").text(_DEF).pop();
		xml.element("SKills");
		for (Skill S : skills) {
			xml.element("Skill").text(S.id).pop();
		}
		xml.pop();
		xml.element("States");
		for (Status S : states) {
			xml.element("Status").text(S.id).pop();
		}
		xml.pop();
		xml.element("Weapon").text(weapon.id).pop();
		xml.element("Armor").text(armor.id).pop();
		xml.pop();
	}

	@Override
	public Actor xmlRead(Element element) {
		Klass klass = Database.classes[element.getInt("Class")];
		Actor actor = new Actor(klass, 5);
		actor.Name = element.get("Name");
		actor.playable_character = element.getBoolean("PC");
		actor.LEVEL = element.getInt("LVL");
		actor.EXP = element.getInt("EXP");
		actor._maxhp = element.getInt("HPP");
		actor._maxsp = element.getInt("SPP");
		actor.HP = element.getInt("HP");
		actor.SP = element.getInt("SP");
		actor._POW = element.getInt("POW");
		actor._SKL = element.getInt("SKL");
		actor._MND = element.getInt("MND");
		actor._MOV = element.getInt("MOV");
		actor._VSN = element.getInt("VSN");
		actor._SPD = element.getInt("SPD");
		actor._ATK = element.getInt("ATK");
		actor._DEF = element.getInt("DEF");
		actor.skills = new ArrayList<Skill>();
		for (Element E : element.getChildrenByName("Skills")) {
			skills.add(Database.skills[E.getInt("Skill")]);
		}
		actor.states = new ArrayList<Status>();
		for (Element E : element.getChildrenByName("States")) {
			states.add(Database.states[E.getInt("Status")]);
		}
		actor.weapon = Database.weapons[element.getInt("Weapon")];
		actor.armor = Database.armors[element.getInt("Armor")];
		return actor;
	}

	public float get_atk_modifier() {
		return get_modifier(0);
	}

	public float get_def_modifier() {
		return get_modifier(1);
	}

	public float get_spd_modifier() {
		return get_modifier(2);
	}

	public float get_eva_modifier() {
		return get_modifier(4) - 1.0f;
	}

	public float get_acc_modifier() {
		return get_modifier(3) - 1.0f;
	}

	public void reset_stat_modifiers() {
		for (int i = 0; i < stat_modifiers.length; i++) {
			stat_modifiers[i] = 0;
		}
	}

	/**
	 * returns a value from 0.25 - 1.75 indicating a multipler for certain
	 * stats.
	 * 
	 * @param value
	 * @return
	 */
	private float get_modifier(int value) {
		int r = Math.max(Math.min(stat_modifiers[value], 5), -5);
		return (0.15f * r) + 1.0f;
	}
}
